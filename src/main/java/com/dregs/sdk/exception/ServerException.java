package com.dregs.sdk.exception;

/**
 * 5xx. Something went wrong inside Dregs.
 *
 * <p>These are retried automatically, so one reaching you means every attempt failed. Nothing about
 * the request needs changing; try again later, or report it with the request id.
 */
public class ServerException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a 5xx exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     */
    public ServerException(String message, int statusCode, Object body, String requestId) {
        super(message, statusCode, body, requestId);
    }
}
