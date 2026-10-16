package com.dregs.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which events an {@link Events#list(EventListParams)} call returns.
 *
 * <pre>{@code
 * client.events().list(EventListParams.builder().identity("user_12345").build());
 * }</pre>
 *
 * <p>Sort on fields such as {@code "timestamp"} or {@code "eventName"}. The default is newest
 * first.
 */
public final class EventListParams extends ListParams {

    private final String identity;
    private final String group;
    private final String groupType;
    private final String device;
    private final String session;
    private final String term;

    private EventListParams(Builder builder) {
        super(builder);

        this.identity = builder.identity;
        this.group = builder.group;
        this.groupType = builder.groupType;
        this.device = builder.device;
        this.session = builder.session;
        this.term = builder.term;
    }

    /**
     * Starts building the parameters.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The identity filter.
     *
     * @return the identity id, or {@code null}
     */
    public String identity() {
        return identity;
    }

    /**
     * The group filter.
     *
     * @return the group id, or {@code null}
     */
    public String group() {
        return group;
    }

    /**
     * The group type filter.
     *
     * @return the group type, or {@code null}
     */
    public String groupType() {
        return groupType;
    }

    /**
     * The device filter.
     *
     * @return the device fingerprint, or {@code null}
     */
    public String device() {
        return device;
    }

    /**
     * The session filter.
     *
     * @return the browser session, or {@code null}
     */
    public String session() {
        return session;
    }

    /**
     * The free-text filter.
     *
     * @return the term, or {@code null}
     */
    public String term() {
        return term;
    }

    @Override
    Map<String, Object> filters() {
        Map<String, Object> filters = new LinkedHashMap<>();

        filters.put("identity", identity);
        filters.put("group", group);
        filters.put("groupType", groupType);
        filters.put("device", device);
        filters.put("session", session);
        filters.put("term", term);

        return filters;
    }

    /**
     * Builds an {@link EventListParams}.
     *
     * <p>Obtain one from {@link EventListParams#builder()}.
     */
    public static final class Builder extends ListParams.Builder<Builder> {

        private String identity;
        private String group;
        private String groupType;
        private String device;
        private String session;
        private String term;

        private Builder() {
        }

        @Override
        Builder self() {
            return this;
        }

        /**
         * Only events from an identity.
         *
         * @param identity the identity's id
         * @return this builder
         */
        public Builder identity(String identity) {
            this.identity = identity;

            return this;
        }

        /**
         * Only events sent with a group.
         *
         * @param group the group's id
         * @return this builder
         */
        public Builder group(String group) {
            this.group = group;

            return this;
        }

        /**
         * Only events sent with a group of a type.
         *
         * @param groupType the type, such as {@code "team"}
         * @return this builder
         */
        public Builder groupType(String groupType) {
            this.groupType = groupType;

            return this;
        }

        /**
         * Only events from a device.
         *
         * @param device the device's fingerprint
         * @return this builder
         */
        public Builder device(String device) {
            this.device = device;

            return this;
        }

        /**
         * Only events from a browser session.
         *
         * @param session the session
         * @return this builder
         */
        public Builder session(String session) {
            this.session = session;

            return this;
        }

        /**
         * Only events matching free text.
         *
         * @param term text matched against event names and data
         * @return this builder
         */
        public Builder term(String term) {
            this.term = term;

            return this;
        }

        /**
         * Validates and builds the parameters.
         *
         * @return the parameters
         * @throws IllegalArgumentException when the page is negative or the order is not
         *     {@code "asc"} or {@code "desc"}
         */
        public EventListParams build() {
            validate();

            return new EventListParams(this);
        }
    }
}
