package com.dregs.exception;

/**
 * 404. No such identity, or no analysis has been run for it yet.
 *
 * <p>An identity exists from the moment the first event naming it is recorded, so this on a
 * {@code scores} call usually means the id does not match the one you track under. On an
 * {@code analysis} call it more often means scoring has not run yet.
 */
public class NotFoundException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a 404 exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     */
    public NotFoundException(String message, int statusCode, Object body, String requestId) {
        super(message, statusCode, body, requestId);
    }
}
