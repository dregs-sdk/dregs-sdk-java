package com.dregs.exception;

/**
 * Base class for everything the Dregs SDK throws.
 *
 * <p>A caller that only wants a coarse "the Dregs call failed" branch can catch this one class.
 * Errors that came back from the API are {@link DregsApiException}s and carry the HTTP status and
 * the parsed body; errors that never reached the API (DNS, a refused connection, a timeout) are
 * {@link DregsConnectionException}s instead.
 *
 * <p>These are unchecked. A fraud score is an input to a decision rather than a step that must
 * succeed, so the SDK leaves it to you to decide where the failure is worth handling instead of
 * forcing a {@code catch} at every call site.
 */
public class DregsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message what went wrong
     */
    public DregsException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and an underlying cause.
     *
     * @param message what went wrong
     * @param cause the exception this one is wrapping
     */
    public DregsException(String message, Throwable cause) {
        super(message, cause);
    }
}
