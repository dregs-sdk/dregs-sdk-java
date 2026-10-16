package com.dregs.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A change to an identity, for {@link Identities#update(String, IdentityUpdate)}.
 *
 * <pre>{@code
 * client.identities().update("qa_bot_7", IdentityUpdate.builder().disregarded(true).build());
 * }</pre>
 *
 * <p>Only what you set is sent, and at least one of the two has to be.
 */
public final class IdentityUpdate {

    private final Boolean disregarded;
    private final Map<String, Object> attributes;

    private IdentityUpdate(Builder builder) {
        this.disregarded = builder.disregarded;
        this.attributes = builder.attributes == null ? null : TrackRequest.copyOf(builder.attributes);
    }

    /**
     * Starts building an update.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Whether the identity is to be disregarded.
     *
     * @return the new setting, or {@code null} to leave it as it is
     */
    public Boolean disregarded() {
        return disregarded;
    }

    /**
     * Attributes to merge into the identity.
     *
     * @return the attributes, or {@code null} to leave them as they are
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    /** The {@code PATCH} body: only the parts that were set. */
    Map<String, Object> body() {
        Map<String, Object> body = new LinkedHashMap<>();

        if (disregarded != null) {
            body.put("disregarded", disregarded);
        }

        if (attributes != null) {
            body.put("attributes", attributes);
        }

        return body;
    }

    @Override
    public String toString() {
        return "IdentityUpdate[disregarded=" + disregarded + ", attributes=" + attributes + "]";
    }

    /**
     * Builds an {@link IdentityUpdate}.
     *
     * <p>Obtain one from {@link IdentityUpdate#builder()}.
     */
    public static final class Builder {

        private Boolean disregarded;
        private Map<String, Object> attributes;

        private Builder() {
        }

        /**
         * Sets whether the identity is disregarded.
         *
         * <p>A disregarded identity is left out of scoring and escalations, which is what to do with
         * your own staff and test accounts. The identities linked to it are re-scored.
         *
         * @param disregarded true to disregard the identity, false to score it again
         * @return this builder
         */
        public Builder disregarded(boolean disregarded) {
            this.disregarded = disregarded;

            return this;
        }

        /**
         * Sets attributes to merge into the identity, as {@link TrackRequest.Builder#identityData}
         * would.
         *
         * @param attributes the attributes
         * @return this builder
         */
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;

            return this;
        }

        /**
         * Validates and builds the update.
         *
         * @return the update
         * @throws IllegalArgumentException when neither {@code disregarded} nor {@code attributes}
         *     was set
         */
        public IdentityUpdate build() {
            if (disregarded == null && attributes == null) {
                throw new IllegalArgumentException("Pass disregarded, attributes, or both.");
            }

            return new IdentityUpdate(this);
        }
    }
}
