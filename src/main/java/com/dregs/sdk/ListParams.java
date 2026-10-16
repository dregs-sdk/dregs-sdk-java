package com.dregs.sdk;

import java.util.Locale;
import java.util.Map;

/**
 * What every list call shares: which page, how big, and in what order.
 *
 * <p>Each namespace has its own subclass carrying its filters, such as
 * {@link IdentityListParams}. Like {@link TrackRequest}, they are built rather than constructed,
 * because a list call's options are all optional and Java has no keyword arguments:
 *
 * <pre>{@code
 * client.identities().list(IdentityListParams.builder()
 *         .term("example.com")
 *         .pageSize(50)
 *         .sort("humanityScore")
 *         .order("asc")
 *         .build());
 * }</pre>
 *
 * <p>Anything left unset is not sent, so the API's own default applies.
 */
public abstract class ListParams {

    private final int page;
    private final Integer pageSize;
    private final String sort;
    private final String order;

    ListParams(Builder<?> builder) {
        this.page = builder.page;
        this.pageSize = builder.pageSize;
        this.sort = builder.sort;
        this.order = builder.order == null ? null : builder.order.toUpperCase(Locale.ROOT);
    }

    /**
     * The page to return, counting from 0.
     *
     * <p>A {@code listAll} call starts its walk here, so it is normally left at 0.
     *
     * @return the page number
     */
    public int page() {
        return page;
    }

    /**
     * Items per page.
     *
     * @return the page size, or {@code null} for the API's default
     */
    public Integer pageSize() {
        return pageSize;
    }

    /**
     * The field to sort on.
     *
     * @return the sort field, or {@code null} for the API's default
     */
    public String sort() {
        return sort;
    }

    /**
     * The sort direction, as sent.
     *
     * @return {@code "ASC"}, {@code "DESC"}, or {@code null} for the API's default
     */
    public String order() {
        return order;
    }

    /** The namespace's own filters, by query parameter name. Unset ones are null. */
    abstract Map<String, Object> filters();

    /**
     * The settings every list call shares.
     *
     * <p>Every setter returns the same builder, and nothing is validated until {@code build()}.
     *
     * @param <B> the concrete builder, so a chain of setters keeps its type
     */
    public abstract static class Builder<B extends Builder<B>> {

        private int page;
        private Integer pageSize;
        private String sort;
        private String order;

        Builder() {
        }

        abstract B self();

        /**
         * Sets the page to return.
         *
         * @param page the page number, counting from 0
         * @return this builder
         */
        public B page(int page) {
            this.page = page;

            return self();
        }

        /**
         * Sets how many items a page holds.
         *
         * @param pageSize items per page, at most 100
         * @return this builder
         */
        public B pageSize(int pageSize) {
            this.pageSize = pageSize;

            return self();
        }

        /**
         * Sets the field to sort on.
         *
         * @param sort the field, such as {@code "lastTrackedAt"}; each namespace documents its own
         * @return this builder
         */
        public B sort(String sort) {
            this.sort = sort;

            return self();
        }

        /**
         * Sets the sort direction.
         *
         * @param order {@code "asc"} or {@code "desc"}, in either case
         * @return this builder
         */
        public B order(String order) {
            this.order = order;

            return self();
        }

        /** Checks the shared settings, so a bad one is refused before any request is made. */
        void validate() {
            if (page < 0) {
                throw new IllegalArgumentException("Pages are numbered from 0.");
            }

            if (order != null
                    && !order.equalsIgnoreCase("asc")
                    && !order.equalsIgnoreCase("desc")) {
                throw new IllegalArgumentException("order is 'asc' or 'desc'.");
            }
        }
    }
}
