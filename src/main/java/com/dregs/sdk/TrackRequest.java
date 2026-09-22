package com.dregs.sdk;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One backend event, ready to send.
 *
 * <p>Java has no keyword arguments, so the optional parts of an event arrive through a builder
 * rather than through an overload nobody can read at the call site:
 *
 * <pre>{@code
 * client.track(TrackRequest.builder("user.signup", "user_12345")
 *         .data(Map.of("plan", "pro", "referrer", "partner-x"))
 *         .identityData(Map.of("email", "ada@example.com", "name", "Ada Lovelace"))
 *         .eventId("signup-991")
 *         .build());
 * }</pre>
 *
 * <p>For an event with nothing but a type and an identity, {@code client.track("user.signup",
 * "user_12345")} says the same thing in one line.
 */
public final class TrackRequest {

    /** The longest event id Dregs accepts. */
    public static final int MAX_EVENT_ID_LENGTH = 64;

    /** The prefix Dregs reserves for the event ids it generates itself. */
    public static final String RESERVED_EVENT_ID_PREFIX = "dregs-";

    private final String eventType;
    private final String identity;
    private final Map<String, Object> data;
    private final Map<String, Object> identityData;
    private final String eventId;
    private final Instant timestamp;
    private final String source;

    private TrackRequest(Builder builder) {
        this.eventType = builder.eventType;
        this.identity = builder.identity;
        this.data = copyOf(builder.data);
        this.identityData = copyOf(builder.identityData);
        this.eventId = builder.eventId;
        this.timestamp = builder.timestamp;
        this.source = builder.source;
    }

    /**
     * Starts building an event.
     *
     * @param eventType your name for the event, such as {@code "user.signup"}
     * @param identity your own id for the user the event belongs to
     * @return a builder
     */
    public static Builder builder(String eventType, String identity) {
        return new Builder(eventType, identity);
    }

    /**
     * An event with nothing but a type and an identity.
     *
     * @param eventType your name for the event, such as {@code "user.signup"}
     * @param identity your own id for the user the event belongs to
     * @return the request
     * @throws IllegalArgumentException when either argument is empty
     */
    public static TrackRequest of(String eventType, String identity) {
        return builder(eventType, identity).build();
    }

    /**
     * Your name for the event.
     *
     * @return the event type
     */
    public String eventType() {
        return eventType;
    }

    /**
     * Your own id for the user the event belongs to.
     *
     * @return the identity id
     */
    public String identity() {
        return identity;
    }

    /**
     * Attributes of the event itself.
     *
     * @return the event data, empty when none was set
     */
    public Map<String, Object> data() {
        return data;
    }

    /**
     * Attributes of the user.
     *
     * @return the identity data, empty when none was set
     */
    public Map<String, Object> identityData() {
        return identityData;
    }

    /**
     * Your own id for the event, when you supplied one.
     *
     * @return the event id, or {@code null} to have the SDK generate one
     */
    public String eventId() {
        return eventId;
    }

    /**
     * When the event happened, when you supplied it.
     *
     * @return the timestamp, or {@code null} to let Dregs record the arrival time
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * The label for where the event came from.
     *
     * @return the source, or {@code null} for the SDK's own default
     */
    public String source() {
        return source;
    }

    @Override
    public String toString() {
        return "TrackRequest[eventType=" + eventType + ", identity=" + identity + ", eventId="
                + eventId + "]";
    }

    private static Map<String, Object> copyOf(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        // Not Map.copyOf: a caller may well pass a null value for an attribute they do not have,
        // and throwing on that would be a strange way to find out.
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /**
     * Builds a {@link TrackRequest}.
     *
     * <p>Obtain one from {@link TrackRequest#builder(String, String)}. Every setter returns the
     * same builder, and nothing is validated until {@link #build()}.
     */
    public static final class Builder {

        private final String eventType;
        private final String identity;
        private Map<String, Object> data;
        private Map<String, Object> identityData;
        private String eventId;
        private Instant timestamp;
        private String source;

        private Builder(String eventType, String identity) {
            this.eventType = eventType;
            this.identity = identity;
        }

        /**
         * Sets attributes of the event itself.
         *
         * <p>Values should be things JSON can carry: strings, numbers, booleans, and nested maps
         * or lists of them.
         *
         * @param data the event's attributes
         * @return this builder
         */
        public Builder data(Map<String, Object> data) {
            this.data = data;

            return this;
        }

        /**
         * Sets attributes of the <em>user</em>, such as email, name, or username.
         *
         * <p>Dregs merges these into the identity, and the analyzers lean on them heavily, so send
         * them whenever you have them. Flat keys work best; name them as your application already
         * does and map them under <b>Settings &rarr; Mappings</b>.
         *
         * @param identityData the user's attributes
         * @return this builder
         */
        public Builder identityData(Map<String, Object> identityData) {
            this.identityData = identityData;

            return this;
        }

        /**
         * Sets your own id for the event, which makes ingestion idempotent.
         *
         * <p>Reposting the same id returns the original event instead of recording a second one.
         * Pass the id your application already has — the row id of the record that triggered the
         * event, say — and a retry after a timeout can never double-count.
         *
         * <p>When you omit it the SDK generates one, which is what makes its own retries safe.
         *
         * @param eventId your id for the event, at most {@value TrackRequest#MAX_EVENT_ID_LENGTH} characters
         *     and not starting with {@value TrackRequest#RESERVED_EVENT_ID_PREFIX}
         * @return this builder
         */
        public Builder eventId(String eventId) {
            this.eventId = eventId;

            return this;
        }

        /**
         * Sets when the event happened, if not now.
         *
         * @param timestamp the moment the event occurred
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;

            return this;
        }

        /**
         * Sets a label for where the event came from.
         *
         * @param source the label, defaulting to {@code "java-sdk"}
         * @return this builder
         */
        public Builder source(String source) {
            this.source = source;

            return this;
        }

        /**
         * Validates and builds the request.
         *
         * @return the request
         * @throws IllegalArgumentException when the event type or identity is missing, or the
         *     event id is reserved or too long
         */
        public TrackRequest build() {
            if (eventType == null || eventType.isEmpty()) {
                throw new IllegalArgumentException("eventType is required.");
            }

            if (identity == null || identity.isEmpty()) {
                throw new IllegalArgumentException(
                        "identity is required. A server-side event has no device signature, so "
                                + "the identity is the only thing tying the event to a user.");
            }

            if (eventId != null) {
                if (eventId.startsWith(RESERVED_EVENT_ID_PREFIX)) {
                    throw new IllegalArgumentException(
                            "Event ids starting with '" + RESERVED_EVENT_ID_PREFIX
                                    + "' are reserved for Dregs itself.");
                }

                if (eventId.length() > MAX_EVENT_ID_LENGTH) {
                    throw new IllegalArgumentException(
                            "Event ids cannot be longer than " + MAX_EVENT_ID_LENGTH
                                    + " characters.");
                }
            }

            return new TrackRequest(this);
        }
    }
}
