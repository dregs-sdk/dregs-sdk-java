package com.dregs.sdk.exception;

/**
 * Dregs answered, and the answer was an error.
 *
 * <p>Every status Dregs uses has its own subclass, so catching a specific failure does not mean
 * inspecting {@link #statusCode()}. The generic class is thrown only for a status this SDK release
 * does not recognize.
 */
public class DregsApiException extends DregsException {

    private static final long serialVersionUID = 1L;

    /** The HTTP status code Dregs answered with.
     *
     * @serial
     */
    private final int statusCode;

    /** The parsed response body, which is whatever JSON shapes the endpoint uses. */
    private final transient Object body;

    /** The value of the {@code X-Request-Id} response header, or null.
     *
     * @serial
     */
    private final String requestId;

    /**
     * Creates an API exception.
     *
     * @param message the human-readable message, taken from the response body when it carried one
     * @param statusCode the HTTP status code
     * @param body the parsed JSON body, or {@code null} when the response was not JSON
     * @param requestId the {@code X-Request-Id} response header, or {@code null} when absent
     */
    public DregsApiException(String message, int statusCode, Object body, String requestId) {
        super(message);

        this.statusCode = statusCode;
        this.body = body;
        this.requestId = requestId;
    }

    /**
     * Returns the HTTP status code Dregs answered with.
     *
     * @return the status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the parsed response body.
     *
     * <p>A JSON object comes back as a {@code Map<String, Object>} and a JSON array as a
     * {@code List<Object>}. It is {@code null} when the response carried no body or was not JSON,
     * which is what a proxy's HTML error page looks like from here.
     *
     * @return the parsed body, or {@code null}
     */
    public Object body() {
        return body;
    }

    /**
     * Returns the request id Dregs assigned, when the response carried one.
     *
     * <p>Quote it when you report a problem: it is what lets support find the request in the logs.
     *
     * @return the {@code X-Request-Id} header value, or {@code null}
     */
    public String requestId() {
        return requestId;
    }

    @Override
    public String toString() {
        String suffix = requestId == null ? "" : " (request " + requestId + ")";

        return "HTTP " + statusCode + ": " + getMessage() + suffix;
    }
}
