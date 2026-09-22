package com.dregs.exception;

/**
 * 400. The request was malformed or missing something Dregs requires.
 *
 * <p>For event ingestion this most often means the event carried neither an identity nor a device,
 * or the body failed validation. Retrying an unchanged request will fail the same way.
 */
public class BadRequestException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a 400 exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     */
    public BadRequestException(String message, int statusCode, Object body, String requestId) {
        super(message, statusCode, body, requestId);
    }
}
