package com.dregs.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which devices a {@link Devices#list(DeviceListParams)} call returns.
 *
 * <pre>{@code
 * client.devices().list(DeviceListParams.builder().identity("user_12345").build());
 * }</pre>
 *
 * <p>Sort on fields such as {@code "lastSeenAt"} or {@code "country"}. The default is most
 * recently seen first.
 */
public final class DeviceListParams extends ListParams {

    private final String identity;
    private final String term;

    private DeviceListParams(Builder builder) {
        super(builder);

        this.identity = builder.identity;
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
        filters.put("term", term);

        return filters;
    }

    /**
     * Builds a {@link DeviceListParams}.
     *
     * <p>Obtain one from {@link DeviceListParams#builder()}.
     */
    public static final class Builder extends ListParams.Builder<Builder> {

        private String identity;
        private String term;

        private Builder() {
        }

        @Override
        Builder self() {
            return this;
        }

        /**
         * Only devices an identity was seen on.
         *
         * @param identity the identity's id
         * @return this builder
         */
        public Builder identity(String identity) {
            this.identity = identity;

            return this;
        }

        /**
         * Only devices matching free text.
         *
         * @param term text matched against fingerprints, addresses, places, and user agents
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
        public DeviceListParams build() {
            validate();

            return new DeviceListParams(this);
        }
    }
}
