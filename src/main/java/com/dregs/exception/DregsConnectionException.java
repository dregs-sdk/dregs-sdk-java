package com.dregs.exception;

/**
 * The request never reached Dregs: DNS, TCP, TLS, or a dropped connection.
 *
 * <p>The SDK retries these before giving up, so seeing one means every attempt failed. Nothing was
 * recorded, and the call is safe to make again.
 */
public class DregsConnectionException extends DregsException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a connection exception.
     *
     * @param message what went wrong, naming the URL that could not be reached
     */
    public DregsConnectionException(String message) {
        super(message);
    }

    /**
     * Creates a connection exception with an underlying cause.
     *
     * @param message what went wrong, naming the URL that could not be reached
     * @param cause the I/O failure underneath
     */
    public DregsConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
