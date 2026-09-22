package com.dregs.sdk.exception;

/**
 * 402. The account is over its monthly event limit and ingestion is refused.
 *
 * <p>Events are not queued while an account is over its limit, so the caller decides whether to
 * drop the event or hold it. The limit resets with the billing period; upgrading the plan clears it
 * immediately.
 */
public class QuotaExceededException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a 402 exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     */
    public QuotaExceededException(String message, int statusCode, Object body, String requestId) {
        super(message, statusCode, body, requestId);
    }
}
