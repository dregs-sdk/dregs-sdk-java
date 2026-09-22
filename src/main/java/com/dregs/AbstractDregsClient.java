package com.dregs;

import com.dregs.exception.AuthenticationException;
import com.dregs.exception.BadRequestException;
import com.dregs.exception.DregsApiException;
import com.dregs.exception.DregsConnectionException;
import com.dregs.exception.DregsTimeoutException;
import com.dregs.exception.NotFoundException;
import com.dregs.exception.PermissionDeniedException;
import com.dregs.exception.QuotaExceededException;
import com.dregs.exception.RateLimitException;
import com.dregs.exception.ServerException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Configuration, request construction, and response handling shared by both clients.
 *
 * <p>The sync and async clients differ only in how they wait for the HTTP call. Everything that
 * decides <em>what</em> to send, <em>how</em> to read the answer, and <em>whether</em> to try again
 * lives here so the two cannot drift.
 */
abstract class AbstractDregsClient {

    static final String DEFAULT_BASE_URL = "https://dregs.com/api";
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    static final int DEFAULT_MAX_RETRIES = 2;

    static final String SECRET_KEY_ENV = "DREGS_SECRET_KEY";
    static final String BASE_URL_ENV = "DREGS_BASE_URL";

    static final String DEFAULT_SOURCE = "java-sdk";

    /**
     * Statuses worth another attempt. 429 and 5xx are transient by definition; 408 shows up in
     * front of some proxies.
     */
    static final Set<Integer> RETRY_STATUSES = Set.of(408, 429, 500, 502, 503, 504);

    /** Body-level statuses an older API build used on {@code POST /api/events}. */
    private static final String STATUS_RATE_LIMITED = "rate_limited";

    private static final String STATUS_QUOTA_EXCEEDED = "quota_exceeded";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    /**
     * How environment variables are read. Swapped in tests, because a JVM cannot set its own
     * environment and the configuration rules are worth testing.
     */
    static UnaryOperator<String> environment = System::getenv;

    private final String secretKey;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;

    private final RetryBackoff backoff;

    AbstractDregsClient(
            String secretKey, String baseUrl, Duration timeout, int maxRetries, RetryBackoff backoff) {
        this.secretKey = resolveSecretKey(secretKey);
        this.baseUrl = resolveBaseUrl(baseUrl);
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.maxRetries = requireNonNegative(maxRetries);
        this.backoff = backoff == null ? RetryBackoff.DEFAULT : backoff;
    }

    /**
     * The API root every request is built against.
     *
     * @return the base URL, without a trailing slash
     */
    public final String baseUrl() {
        return baseUrl;
    }

    /**
     * How many times a failed request is retried before the error is thrown.
     *
     * @return the retry ceiling
     */
    public final int maxRetries() {
        return maxRetries;
    }

    /**
     * How long a single attempt may take.
     *
     * @return the per-request timeout
     */
    public final Duration timeout() {
        return timeout;
    }

    // -- configuration ------------------------------------------------------------------------

    private static String resolveSecretKey(String explicit) {
        String resolved = explicit != null && !explicit.isEmpty() ? explicit : environment.apply(SECRET_KEY_ENV);

        if (resolved == null || resolved.isEmpty()) {
            throw new IllegalArgumentException(
                    "No Dregs secret key. Pass secretKey(...) to the builder or set the "
                            + SECRET_KEY_ENV + " environment variable. You will find your "
                            + "credential's secret key under Settings -> Credentials in the Dregs "
                            + "dashboard.");
        }

        if (resolved.startsWith("pk_")) {
            throw new IllegalArgumentException(
                    "That is a public key. The public key is for the browser tracker and cannot "
                            + "read identities or scores; this SDK needs the secret key from the "
                            + "same credential, which starts with 'sk_'.");
        }

        return resolved;
    }

    private static String resolveBaseUrl(String explicit) {
        String resolved = explicit != null && !explicit.isEmpty() ? explicit : environment.apply(BASE_URL_ENV);

        if (resolved == null || resolved.isEmpty()) {
            resolved = DEFAULT_BASE_URL;
        }

        while (resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }

        return resolved;
    }

    private static int requireNonNegative(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative.");
        }

        return maxRetries;
    }

    static HttpClient defaultHttpClient(Duration timeout) {
        return HttpClient.newBuilder()
                .connectTimeout(timeout == null ? DEFAULT_TIMEOUT : timeout)
                // Deliberately NEVER. Every request carries the secret key in an Authorization
                // header, and following a redirect would hand that key to whatever host the
                // redirect named. The API does not redirect, so one means the base URL is wrong.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    static String userAgent() {
        return "dregs-java/" + Dregs.VERSION + " (java " + System.getProperty("java.version") + ")";
    }

    // -- requests -----------------------------------------------------------------------------

    final HttpRequest buildRequest(String method, String path, Map<String, Object> body) {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/" + stripLeadingSlash(path)))
                .timeout(timeout)
                .header("Authorization", "Bearer " + secretKey)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent());

        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(
                            method,
                            HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8));
        }

        return request.build();
    }

    private static String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /**
     * Builds the {@code POST /api/events} body.
     *
     * <p>An event id is always sent. When the caller has an id of their own it is used verbatim,
     * so reposting the same event is a no-op on the Dregs side; otherwise one is generated here,
     * once per call, which is what makes this client's own retries safe to perform.
     */
    final Map<String, Object> trackBody(TrackRequest request) {
        String eventId = request.eventId() != null
                ? request.eventId()
                : UUID.randomUUID().toString().replace("-", "");

        Map<String, Object> identity = new LinkedHashMap<>();

        identity.put("id", request.identity());
        identity.put("data", request.identityData());

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("id", eventId);
        body.put("type", request.eventType());
        body.put("data", request.data());
        body.put("identity", identity);
        body.put("source", request.source() == null ? DEFAULT_SOURCE : request.source());

        if (request.timestamp() != null) {
            body.put("timestamp", formatTimestamp(request.timestamp()));
        }

        return body;
    }

    static String formatTimestamp(Instant value) {
        return TIMESTAMP_FORMAT.format(value);
    }

    /**
     * Percent-encodes one path segment.
     *
     * <p>Identity ids are the caller's own user ids and routinely contain characters that need
     * escaping, an email address being the common one. {@code URLEncoder} is the wrong tool here:
     * it writes a space as {@code +}, which a path reads as a literal plus.
     */
    static String encodeSegment(String segment) {
        StringBuilder encoded = new StringBuilder(segment.length());

        for (byte b : segment.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);

            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-'
                    || c == '.'
                    || c == '_'
                    || c == '~') {
                encoded.append(c);
            } else {
                encoded.append('%').append(String.format("%02X", b & 0xFF));
            }
        }

        return encoded.toString();
    }

    // -- responses ----------------------------------------------------------------------------

    /**
     * Turns a response into parsed JSON, or throws the matching exception.
     *
     * @return the parsed body, which may be a map, a list, or null
     */
    final Object process(HttpResponse<String> response) {
        Object payload = Json.parseOrNull(response.body());

        if (response.statusCode() >= 300) {
            throw apiError(response, payload);
        }

        // An older API build reported both of these as HTTP 200 with the outcome in the body.
        // Reading the body as well as the status keeps this SDK correct against either.
        if (payload instanceof Map<?, ?> body) {
            Object status = body.get("status");

            if (STATUS_RATE_LIMITED.equals(status)) {
                throw new RateLimitException(
                        "Ingestion rate limit exceeded for this credential.",
                        429,
                        payload,
                        headerOrNull(response, "X-Request-Id"),
                        retryAfter(response));
            }

            if (STATUS_QUOTA_EXCEEDED.equals(status)) {
                throw new QuotaExceededException(
                        "The account is over its monthly event limit.",
                        402,
                        payload,
                        headerOrNull(response, "X-Request-Id"));
            }
        }

        return payload;
    }

    private static DregsApiException apiError(HttpResponse<String> response, Object payload) {
        int status = response.statusCode();
        String message = status < 400 ? redirectMessage(response) : messageFrom(payload);
        String requestId = headerOrNull(response, "X-Request-Id");

        return switch (status) {
            case 400 -> new BadRequestException(message, status, payload, requestId);
            case 401 -> new AuthenticationException(message, status, payload, requestId);
            case 402 -> new QuotaExceededException(message, status, payload, requestId);
            case 403 -> new PermissionDeniedException(message, status, payload, requestId);
            case 404 -> new NotFoundException(message, status, payload, requestId);
            case 429 -> new RateLimitException(message, status, payload, requestId, retryAfter(response));
            default -> status >= 500
                    ? new ServerException(message, status, payload, requestId)
                    : new DregsApiException(message, status, payload, requestId);
        };
    }

    /** A 3xx only reaches here because the SDK refuses to follow one. Say why, and where to. */
    private static String redirectMessage(HttpResponse<String> response) {
        String location = headerOrNull(response, "Location");

        return "Dregs answered with a redirect"
                + (location == null ? "" : " to " + location)
                + ". The SDK does not follow redirects, because that would forward your secret key "
                + "to another host. Point the base URL at the final address instead.";
    }

    private static String messageFrom(Object payload) {
        if (payload instanceof Map<?, ?> body) {
            for (String key : new String[] {"message", "error", "status"}) {
                Object value = body.get(key);

                if (value instanceof String text && !text.isEmpty()) {
                    return text;
                }
            }
        }

        return "Request failed";
    }

    private static String headerOrNull(HttpResponse<String> response, String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    private static Double retryAfter(HttpResponse<String> response) {
        Optional<String> raw = response.headers().firstValue("Retry-After");

        if (raw.isEmpty()) {
            return null;
        }

        try {
            return Double.valueOf(raw.get().trim());
        } catch (NumberFormatException ignored) {
            // The header also allows an HTTP date, which is rare enough here that falling back to
            // the client's own backoff beats dragging in a date parser.
            return null;
        }
    }

    // -- retries ------------------------------------------------------------------------------

    final boolean shouldRetry(int attempt, Integer statusCode) {
        if (attempt >= maxRetries) {
            return false;
        }

        return statusCode == null || RETRY_STATUSES.contains(statusCode);
    }

    final long backoffMillis(int attempt, Double retryAfterSeconds) {
        return backoff.millis(attempt, retryAfterSeconds);
    }

    static Double retryAfterOf(DregsApiException exception) {
        return exception instanceof RateLimitException limit ? limit.retryAfter() : null;
    }

    /** Turns a transport failure into the SDK's own exception, keeping timeouts distinguishable. */
    static DregsConnectionException transportError(Throwable cause, URI uri) {
        if (cause instanceof HttpTimeoutException) {
            return new DregsTimeoutException("Request to " + uri + " timed out.", cause);
        }

        if (cause instanceof IOException) {
            return new DregsConnectionException(
                    "Could not reach Dregs at " + uri + ": " + cause.getMessage(), cause);
        }

        return new DregsConnectionException("Request to " + uri + " failed: " + cause, cause);
    }
}
