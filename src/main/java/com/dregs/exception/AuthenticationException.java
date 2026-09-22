package com.dregs.exception;

/**
 * 401. The secret key was missing, unrecognized, revoked, or expired.
 *
 * <p>Check that the key is the {@code sk_} secret key of a credential that still exists under
 * <b>Settings &rarr; Credentials</b>, and that it belongs to the environment you are pointing at.
 */
public class AuthenticationException extends DregsApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a 401 exception.
     *
     * @param message the message from the response body
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null}
     * @param requestId the {@code X-Request-Id} response header, or {@code null}
     */
    public AuthenticationException(String message, int statusCode, Object body, String requestId) {
        super(message, statusCode, body, requestId);
    }
}
