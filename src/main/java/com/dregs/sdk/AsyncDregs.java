package com.dregs.sdk;

import com.dregs.sdk.exception.DregsApiException;
import com.dregs.sdk.model.TrackResult;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * An asynchronous Dregs client.
 *
 * <p>Identical to {@link Dregs} in every respect but the waiting:
 *
 * <pre>{@code
 * AsyncDregs client = AsyncDregs.builder().build();
 *
 * client.track("user.signup", "user_12345")
 *         .thenCompose(result -> client.identities().scores("user_12345"))
 *         .thenAccept(scores -> log.info("humanity {}", scores.humanity()));
 * }</pre>
 *
 * <p>Failures arrive the way {@link CompletableFuture} delivers every failure: the future completes
 * exceptionally, and {@code join()} wraps the cause in a {@link CompletionException}. The cause is
 * the same {@link com.dregs.sdk.exception.DregsException} the synchronous client would have thrown, so
 * a handler written against one works against the other once it unwraps.
 *
 * <p>Retries happen on the JDK's delayed executor rather than by blocking a thread, so an
 * outstanding retry costs nothing while it waits.
 */
public final class AsyncDregs extends AbstractDregsClient implements AutoCloseable {

    private final HttpClient http;
    private final boolean ownsHttpClient;
    private final AsyncIdentities identities;

    private AsyncDregs(Builder builder) {
        super(builder.secretKey, builder.baseUrl, builder.timeout, builder.maxRetries, builder.backoff);

        this.ownsHttpClient = builder.httpClient == null;
        this.http = builder.httpClient == null ? defaultHttpClient(builder.timeout) : builder.httpClient;
        this.identities = new AsyncIdentities(this);
    }

    /**
     * Starts building a client.
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
    public AsyncIdentities identities() {
        return identities;
    }

    /**
     * Records a backend event against an identity. See {@link Dregs#track(String, String)}.
     *
     * @param eventType your name for the event, such as {@code "user.signup"}
     * @param identity your own id for the user
     * @return a future carrying what Dregs did with the event
     * @throws IllegalArgumentException when either argument is empty, thrown before anything is
     *     sent rather than delivered through the future
     */
    public CompletableFuture<TrackResult> track(String eventType, String identity) {
        return track(TrackRequest.of(eventType, identity));
    }

    /**
     * Records a backend event against an identity. See {@link Dregs#track(TrackRequest)}.
     *
     * @param request the event to record
     * @return a future carrying what Dregs did with the event
     */
    public CompletableFuture<TrackResult> track(TrackRequest request) {
        return request("POST", "/events", trackBody(request))
                .thenApply(payload -> TrackResult.fromApi(Json.asObject(payload)));
    }

    /**
     * Sends a request, retrying what is worth retrying.
     *
     * <p>The body is built once, before the first attempt, so a retry re-sends the same event id
     * and cannot record the event twice.
     */
    CompletableFuture<Object> request(String method, String path, Map<String, Object> body) {
        return attempt(buildRequest(method, path, body), 0);
    }

    private CompletableFuture<Object> attempt(HttpRequest httpRequest, int attempt) {
        return http.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .handle((response, failure) -> {
                    if (failure != null) {
                        Throwable cause = unwrap(failure);

                        if (!shouldRetry(attempt, null)) {
                            return CompletableFuture.<Object>failedFuture(
                                    transportError(cause, httpRequest.uri()));
                        }

                        return retry(httpRequest, attempt, backoffMillis(attempt, null));
                    }

                    try {
                        return CompletableFuture.completedFuture(process(response));
                    } catch (DregsApiException exception) {
                        if (!shouldRetry(attempt, exception.statusCode())) {
                            return CompletableFuture.<Object>failedFuture(exception);
                        }

                        return retry(
                                httpRequest, attempt, backoffMillis(attempt, retryAfterOf(exception)));
                    }
                })
                .thenCompose(next -> next);
    }

    private CompletableFuture<Object> retry(HttpRequest httpRequest, int attempt, long delayMillis) {
        if (delayMillis <= 0) {
            return attempt(httpRequest, attempt + 1);
        }

        Executor later = CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS);

        return CompletableFuture.supplyAsync(() -> null, later)
                .thenCompose(ignored -> attempt(httpRequest, attempt + 1));
    }

    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause()
                : failure;
    }

    /**
     * Releases the underlying connection pool, unless you supplied your own {@link HttpClient}.
     *
     * <p>See {@link Dregs#close()}: this is a no-op on Java 17, where {@code HttpClient} is not yet
     * closeable.
     */
    @Override
    public void close() {
        if (!ownsHttpClient) {
            return;
        }

        if (http instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Nothing a caller could usefully do about a pool that will not shut down.
            }
        }
    }

    /**
     * Builds an {@link AsyncDregs} client.
     *
     * <p>Obtain one from {@link AsyncDregs#builder()}. The settings are the same as
     * {@link Dregs.Builder}'s.
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
        public AsyncDregs build() {
            return new AsyncDregs(this);
        }
    }
}
