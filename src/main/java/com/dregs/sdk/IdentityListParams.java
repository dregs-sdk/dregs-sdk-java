package com.dregs.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which identities a {@link Identities#list(IdentityListParams)} call returns.
 *
 * <pre>{@code
 * client.identities().list(IdentityListParams.builder().group("org_678").pageSize(50).build());
 * }</pre>
 *
 * <p>Sort on fields such as {@code "lastTrackedAt"} or {@code "humanityScore"}. The default is most
 * recently updated first.
 */
public final class IdentityListParams extends ListParams {

    private final String term;
    private final String group;
    private final String groupType;
    private final String device;
    private final String badge;

    private IdentityListParams(Builder builder) {
        super(builder);

        this.term = builder.term;
        this.group = builder.group;
        this.groupType = builder.groupType;
        this.device = builder.device;
        this.badge = builder.badge;
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
     * The free-text filter.
     *
     * @return the term, or {@code null}
     */
    public String term() {
        return term;
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
     * The badge filter.
     *
     * @return the badge slug, or {@code null}
     */
    public String badge() {
        return badge;
    }

    @Override
    Map<String, Object> filters() {
        Map<String, Object> filters = new LinkedHashMap<>();

        filters.put("term", term);
        filters.put("group", group);
        filters.put("groupType", groupType);
        filters.put("device", device);
        filters.put("badge", badge);

        return filters;
    }

    /**
     * Builds an {@link IdentityListParams}.
     *
     * <p>Obtain one from {@link IdentityListParams#builder()}.
     */
    public static final class Builder extends ListParams.Builder<Builder> {

        private String term;
        private String group;
        private String groupType;
        private String device;
        private String badge;

        private Builder() {
        }

        @Override
        Builder self() {
            return this;
        }

        /**
         * Only identities matching free text.
         *
         * @param term text matched against ids, names, emails, and usernames
         * @return this builder
         */
        public Builder term(String term) {
            this.term = term;

            return this;
        }

        /**
         * Only members of a group.
         *
         * @param group the group's id. Pair it with {@link #groupType(String)} when your group ids
         *     are not unique across types
         * @return this builder
         */
        public Builder group(String group) {
            this.group = group;

            return this;
        }

        /**
         * Only members of groups of a type.
         *
         * @param groupType the type, such as {@code "team"}
         * @return this builder
         */
        public Builder groupType(String groupType) {
            this.groupType = groupType;

            return this;
        }

        /**
         * Only identities seen on a device.
         *
         * @param device the device's fingerprint
         * @return this builder
         */
        public Builder device(String device) {
            this.device = device;

            return this;
        }

        /**
         * Only identities carrying a badge.
         *
         * @param badge the badge's slug
         * @return this builder
         */
        public Builder badge(String badge) {
            this.badge = badge;

            return this;
        }

        /**
         * Validates and builds the parameters.
         *
         * @return the parameters
         * @throws IllegalArgumentException when the page is negative or the order is not
         *     {@code "asc"} or {@code "desc"}
         */
        public IdentityListParams build() {
            validate();

            return new IdentityListParams(this);
        }
    }
}
