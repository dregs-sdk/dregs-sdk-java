package com.dregs.sdk.exception;

/**
 * An incoming webhook did not verify against the channel's signing secret.
 *
 * <p>Answer the delivery with a 400 and do not act on the payload. The usual cause is verifying a
 * re-serialized copy of the body rather than the bytes that arrived.
 */
public class WebhookVerificationException extends DregsException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a verification exception.
     *
     * @param message why the webhook did not verify
     */
    public WebhookVerificationException(String message) {
        super(message);
    }

    /**
     * Creates a verification exception with an underlying cause.
     *
     * @param message why the webhook did not verify
     * @param cause the parse failure underneath
     */
    public WebhookVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
