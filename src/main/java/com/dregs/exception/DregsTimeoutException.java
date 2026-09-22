package com.dregs.exception;

/**
 * The request was still outstanding when the configured timeout elapsed.
 *
 * <p>Whether Dregs recorded the event is unknown, which is the reason every event carries an
 * idempotency id: posting it again after a timeout cannot record it twice.
 */
public class DregsTimeoutException extends DregsConnectionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a timeout exception.
     *
     * @param message what timed out
     */
    public DregsTimeoutException(String message) {
        super(message);
    }

    /**
     * Creates a timeout exception with an underlying cause.
     *
     * @param message what timed out
     * @param cause the timeout underneath
     */
    public DregsTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
