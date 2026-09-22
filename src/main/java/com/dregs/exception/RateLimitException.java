package com.dregs.exception;

/**
 * 429. The credential exceeded its request rate limit.
 *
 * <p>The SDK retries these on its own, so one reaching you means the limit outlasted the retries.
 * Back off for {@link #retryAfter()} seconds, or spread the load across the window.
 */
public class RateLimitException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /** Seconds to wait, from the {@code Retry-After} header, or null.
     *
     * @serial
     */
    private final Double retryAfter;

    /**
     * Creates a 429 exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code, normally 429
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     * @param retryAfter seconds to wait, from the {@code Retry-After} header, or {@code null}
     */
    public RateLimitException(
            String message, int statusCode, Object body, String requestId, Double retryAfter) {
        super(message, statusCode, body, requestId);

        this.retryAfter = retryAfter;
    }

    /**
     * Returns how long the server asked you to wait.
     *
     * @return seconds from the {@code Retry-After} header, or {@code null} when it sent none
     */
    public Double retryAfter() {
        return retryAfter;
    }
}
