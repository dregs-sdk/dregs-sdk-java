package com.dregs.sdk;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which escalations an {@link Escalations#list(EscalationListParams)} call returns.
 *
 * <pre>{@code
 * client.escalations().list(EscalationListParams.builder()
 *         .status("open")
 *         .severity("critical")
 *         .build());
 * }</pre>
 *
 * <p>Sort on fields such as {@code "openedAt"} or {@code "severity"}. The default is newest first.
 */
public final class EscalationListParams extends ListParams {

    /** The statuses an escalation moves through, as the API spells them. */
    static final Set<String> STATUSES = Set.of("OPEN", "ACKNOWLEDGED", "CLOSED");

    private final String status;
    private final String severity;
    private final String identity;
    private final Long rule;

    private EscalationListParams(Builder builder) {
        super(builder);

        this.status = builder.status == null ? null : status(builder.status);
        this.severity = builder.severity == null || builder.severity.isEmpty()
                ? null
                : builder.severity.toUpperCase(Locale.ROOT);
        this.identity = builder.identity;
        this.rule = builder.rule;
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
     * The status filter, as sent.
     *
     * @return {@code "OPEN"}, {@code "ACKNOWLEDGED"}, {@code "CLOSED"}, or {@code null}
     */
    public String status() {
        return status;
    }

    /**
     * The severity filter, as sent.
     *
     * @return {@code "INFO"}, {@code "WARNING"}, {@code "CRITICAL"}, or {@code null}
     */
    public String severity() {
        return severity;
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
     * The escalation rule filter.
     *
     * @return the rule's id, or {@code null}
     */
    public Long rule() {
        return rule;
    }

    @Override
    Map<String, Object> filters() {
        Map<String, Object> filters = new LinkedHashMap<>();

        filters.put("status", status);
        filters.put("severity", severity);
        filters.put("identity", identity);
        filters.put("rule", rule);

        return filters;
    }

    /**
     * Upper-cases an escalation status, refusing one the API does not have.
     *
     * @throws IllegalArgumentException when the status is not open, acknowledged, or closed
     */
    static String status(String status) {
        String upper = status == null ? "" : status.toUpperCase(Locale.ROOT);

        if (!STATUSES.contains(upper)) {
            throw new IllegalArgumentException("status is 'open', 'acknowledged', or 'closed'.");
        }

        return upper;
    }

    /**
     * Builds an {@link EscalationListParams}.
     *
     * <p>Obtain one from {@link EscalationListParams#builder()}.
     */
    public static final class Builder extends ListParams.Builder<Builder> {

        private String status;
        private String severity;
        private String identity;
        private Long rule;

        private Builder() {
        }

        @Override
        Builder self() {
            return this;
        }

        /**
         * Only escalations in a status.
         *
         * @param status {@code "open"}, {@code "acknowledged"}, or {@code "closed"}, in either case
         * @return this builder
         */
        public Builder status(String status) {
            this.status = status;

            return this;
        }

        /**
         * Only escalations of a severity.
         *
         * @param severity {@code "info"}, {@code "warning"}, or {@code "critical"}, in either case
         * @return this builder
         */
        public Builder severity(String severity) {
            this.severity = severity;

            return this;
        }

        /**
         * Only escalations about an identity.
         *
         * @param identity the identity's id
         * @return this builder
         */
        public Builder identity(String identity) {
            this.identity = identity;

            return this;
        }

        /**
         * Only escalations opened by an escalation rule.
         *
         * @param rule the rule's id
         * @return this builder
         */
        public Builder rule(long rule) {
            this.rule = rule;

            return this;
        }

        /**
         * Validates and builds the parameters.
         *
         * @return the parameters
         * @throws IllegalArgumentException when the page is negative, the order is not
         *     {@code "asc"} or {@code "desc"}, or the status is not one an escalation has
         */
        public EscalationListParams build() {
            validate();

            return new EscalationListParams(this);
        }
    }
}
