package com.dregs.sdk.exception;

/**
 * 403. The credential authenticated but is not allowed to do this.
 */
public class PermissionDeniedException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a 403 exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     */
    public PermissionDeniedException(String message, int statusCode, Object body, String requestId) {
        super(message, statusCode, body, requestId);
    }
}
