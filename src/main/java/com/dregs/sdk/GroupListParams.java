package com.dregs.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which groups a {@link Groups#list(GroupListParams)} call returns.
 *
 * <pre>{@code
 * client.groups().list(GroupListParams.builder().type("team").identity("user_12345").build());
 * }</pre>
 *
 * <p>Sort on fields such as {@code "displayName"} or {@code "lastTrackedAt"}. The default is most
 * recently active first.
 */
public final class GroupListParams extends ListParams {

    private final String type;
    private final String identity;
    private final String term;

    private GroupListParams(Builder builder) {
        super(builder);

        this.type = builder.type;
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
     * The group type filter.
     *
     * @return the type, or {@code null}
     */
    public String type() {
        return type;
    }

    /**
     * The member filter.
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

        filters.put("type", type);
        filters.put("identity", identity);
        filters.put("term", term);

        return filters;
    }

    /**
     * Builds a {@link GroupListParams}.
     *
     * <p>Obtain one from {@link GroupListParams#builder()}.
     */
    public static final class Builder extends ListParams.Builder<Builder> {

        private String type;
        private String identity;
        private String term;

        private Builder() {
        }

        @Override
        Builder self() {
            return this;
        }

        /**
         * Only groups of a type.
         *
         * @param type the type, such as {@code "team"}
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;

            return this;
        }

        /**
         * Only groups an identity belongs to.
         *
         * @param identity the identity's id
         * @return this builder
         */
        public Builder identity(String identity) {
            this.identity = identity;

            return this;
        }

        /**
         * Only groups matching free text.
         *
         * @param term text matched against group ids and names
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
        public GroupListParams build() {
            validate();

            return new GroupListParams(this);
        }
    }
}
