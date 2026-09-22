package com.dregs.sdk;

import com.dregs.sdk.exception.DregsApiException;
import com.dregs.sdk.exception.DregsConnectionException;
import com.dregs.sdk.model.TrackResult;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * A synchronous Dregs client.
 *
 * <p>The secret key comes from the {@code DREGS_SECRET_KEY} environment variable unless you pass
 * one. Find it under <b>Settings &rarr; Credentials</b> in the dashboard; it is the key starting
 * {@code sk_}, not the {@code pk_} public key the browser tracker uses.
 *
 * <pre>{@code
 * Dregs client = Dregs.builder().build();
 *
 * client.track(TrackRequest.builder("user.signup", "user_12345")
 *         .identityData(Map.of("email", "ada@example.com"))
 *         .build());
 *
 * Scores scores = client.identities().scores("user_12345");
 * }</pre>
 *
 * <p>The client holds a connection pool, so build one at startup and keep it rather than making a
 * new one per request. It is immutable and safe to share across threads.
 */
public final class Dregs extends AbstractDregsClient implements AutoCloseable {

    /**
     * This SDK's version, as it appears in the {@code User-Agent} of every request.
     *
     * <p>The build fails when this disagrees with the published artifact's version.
     */
    public static final String VERSION = "0.1.0";

    private final HttpClient http;
    private final boolean ownsHttpClient;
    private final Identities identities;

    private Dregs(Builder builder) {
        super(builder.secretKey, builder.baseUrl, builder.timeout, builder.maxRetries, builder.backoff);

        this.ownsHttpClient = builder.httpClient == null;
        this.http = builder.httpClient == null ? defaultHttpClient(builder.timeout) : builder.httpClient;
        this.identities = new Identities(this);
    }

    /**
     * Starts building a client.
     *
     * <p>Every setting has a default, so {@code Dregs.builder().build()} is a complete client as
     * long as {@code DREGS_SECRET_KEY} is set.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reading identities, their scores, and their analysis.
     *
     * @return the identities namespace
     */
    public Identities identities() {
        return identities;
    }

    /**
     * Records a backend event against an identity.
     *
     * <p>The short form, for an event with nothing but a type and an identity. Use
     * {@link #track(TrackRequest)} to send event or user attributes, which is what the analyzers
     * actually work from.
     *
     * @param eventType your name for the event, such as {@code "user.signup"}. Map it to one of
     *     Dregs's canonical types under <b>Settings &rarr; Mappings</b> so the analyzers know what
     *     it means
     * @param identity your own id for the user. This is the same id you pass to
     *     {@code dregs.identify()} in the browser tracker, and the one you look scores up by
     * @return what Dregs did with the event
     * @throws IllegalArgumentException when either argument is empty
     * @throws com.dregs.sdk.exception.QuotaExceededException when the account is over its monthly
     *     event limit
     * @throws com.dregs.sdk.exception.RateLimitException when the credential is ingesting too fast
     * @throws com.dregs.sdk.exception.AuthenticationException when the secret key was not recognized
     * @throws com.dregs.sdk.exception.BadRequestException when the event was malformed
     */
    public TrackResult track(String eventType, String identity) {
        return track(TrackRequest.of(eventType, identity));
    }

    /**
     * Records a backend event against an identity.
     *
     * <pre>{@code
     * client.track(TrackRequest.builder("user.signup", "user_12345")
     *         .data(Map.of("plan", "pro", "referrer", "partner-x"))
     *         .identityData(Map.of("email", "ada@example.com", "name", "Ada Lovelace"))
     *         .eventId("signup-991")
     *         .build());
     * }</pre>
     *
     * <p>Scoring is asynchronous, so this returns as soon as the event is recorded. The scores it
     * moves appear moments later rather than on the next line.
     *
     * @param request the event to record
     * @return what Dregs did with the event. Check {@link TrackResult#accepted()} to confirm it
     *     was recorded
     * @throws com.dregs.sdk.exception.QuotaExceededException when the account is over its monthly
     *     event limit. Events are not queued while an account is over it, so this is yours to
     *     drop or hold
     * @throws com.dregs.sdk.exception.RateLimitException when the credential is ingesting too fast
     * @throws com.dregs.sdk.exception.AuthenticationException when the secret key was not recognized
     * @throws com.dregs.sdk.exception.BadRequestException when the event was malformed
     */
    public TrackResult track(TrackRequest request) {
        Object payload = request("POST", "/events", trackBody(request));

        return TrackResult.fromApi(Json.asObject(payload));
    }

    /**
     * Sends a request, retrying what is worth retrying.
     *
     * <p>The body is built once, before the first attempt, so a retry re-sends the same event id
     * and cannot record the event twice.
     */
    Object request(String method, String path, Map<String, Object> body) {
        HttpRequest httpRequest = buildRequest(method, path, body);
        int attempt = 0;

        while (true) {
            long waitMillis;

            try {
                HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                try {
                    return process(response);
                } catch (DregsApiException exception) {
                    if (!shouldRetry(attempt, exception.statusCode())) {
                        throw exception;
                    }

                    waitMillis = backoffMillis(attempt, retryAfterOf(exception));
                }
            } catch (IOException exception) {
                if (!shouldRetry(attempt, null)) {
                    throw transportError(exception, httpRequest.uri());
                }

                waitMillis = backoffMillis(attempt, null);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new DregsConnectionException(
                        "Interrupted while waiting for " + httpRequest.uri() + ".", exception);
            }

            sleep(waitMillis, httpRequest);
            attempt++;
        }
    }

    private static void sleep(long millis, HttpRequest request) {
        if (millis <= 0) {
            return;
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new DregsConnectionException(
                    "Interrupted while backing off before retrying " + request.uri() + ".", exception);
        }
    }

    /**
     * Releases the underlying connection pool, unless you supplied your own {@link HttpClient}.
     *
     * <p>{@code HttpClient} only became closeable in Java 21, so on Java 17 this is a no-op and the
     * pool is released when the client is collected. Either way a Dregs client is meant to outlive
     * the requests it makes, so most applications never call this.
     */
    @Override
    public void close() {
        if (!ownsHttpClient) {
            return;
        }

        // Written as an instanceof rather than a direct call so the same bytecode runs on 17,
        // where HttpClient does not implement AutoCloseable, and on 21 and later, where it does.
        if (http instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Nothing a caller could usefully do about a pool that will not shut down.
            }
        }
    }

    /**
     * Builds a {@link Dregs} client.
     *
     * <p>Obtain one from {@link Dregs#builder()}.
     */
    public static final class Builder {

        private String secretKey;
        private String baseUrl;
        private Duration timeout = DEFAULT_TIMEOUT;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private HttpClient httpClient;

        RetryBackoff backoff;

        private Builder() {
        }

        /**
         * Sets the credential's secret key.
         *
         * @param secretKey the {@code sk_} key, defaulting to {@code $DREGS_SECRET_KEY}
         * @return this builder
         */
        public Builder secretKey(String secretKey) {
            this.secretKey = secretKey;

            return this;
        }

        /**
         * Sets the API root.
         *
         * @param baseUrl the root, defaulting to {@code $DREGS_BASE_URL} and then to
         *     {@code https://dregs.com/api}
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;

            return this;
        }

        /**
         * Sets how long a single attempt may take.
         *
         * @param timeout the per-request timeout, 10 seconds by default
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;

            return this;
        }

        /**
         * Sets how many times a failed request is retried.
         *
         * <p>Retries cover connection failures, timeouts, 429s, and 5xx, with exponential backoff
         * and jitter; the {@code Retry-After} header wins when the server sends one. Pass 0 to
         * handle it yourself.
         *
         * @param maxRetries the retry ceiling, 2 by default
         * @return this builder
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;

            return this;
        }

        /**
         * Supplies an {@link HttpClient} to use instead of the one this client would build.
         *
         * <p>For callers who need their own proxy, TLS settings, or executor. A client you supply
         * is yours to close.
         *
         * @param httpClient the client to send through
         * @return this builder
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;

            return this;
        }

        /** Overrides the retry backoff. Used by the tests, which have no time to sleep. */
        Builder backoff(RetryBackoff backoff) {
            this.backoff = backoff;

            return this;
        }

        /**
         * Builds the client.
         *
         * @return the client
         * @throws IllegalArgumentException when no secret key can be resolved, when the key is a
         *     {@code pk_} public key, or when {@code maxRetries} is negative
         */
        public Dregs build() {
            return new Dregs(this);
        }
    }
}
