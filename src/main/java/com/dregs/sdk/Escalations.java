package com.dregs.sdk;

import com.dregs.sdk.model.Escalation;
import com.dregs.sdk.model.EscalationSummary;
import com.dregs.sdk.model.Page;

/**
 * Reading the escalations your escalation rules open, and moving them along.
 *
 * <p>Reach one through {@link Dregs#escalations()}.
 */
public final class Escalations {

    private final Dregs client;

    Escalations(Dregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of escalations, newest first.
     *
     * @return the page
     */
    public Page<Escalation> list() {
        return list(null);
    }

    /**
     * Returns one page of escalations.
     *
     * <p>Use {@link #listAll(EscalationListParams)} to walk every page.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return the page
     */
    public Page<Escalation> list(EscalationListParams params) {
        ListCall<Escalation> call = Calls.escalationsList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every escalation, fetching pages as the iteration reaches them.
     *
     * @return the escalations
     */
    public Iterable<Escalation> listAll() {
        return listAll(null);
    }

    /**
     * Every escalation that matches, fetching pages as the iteration reaches them.
     *
     * <p>Nothing is fetched until the loop starts, and each new loop starts a fresh walk.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return the escalations
     */
    public Iterable<Escalation> listAll(EscalationListParams params) {
        return client.walk(Calls.escalationsList(params));
    }

    /**
     * Returns the escalation with this id.
     *
     * @param escalationId the escalation's id
     * @return the escalation
     * @throws com.dregs.sdk.exception.NotFoundException when no escalation has this id
     */
    public Escalation get(long escalationId) {
        return client.run(Calls.escalationsGet(escalationId));
    }

    /**
     * Moves the escalation to a status, and returns it.
     *
     * @param escalationId the escalation's id
     * @param status {@code "open"}, {@code "acknowledged"}, or {@code "closed"}, in either case
     * @return the escalation, as it is now
     * @throws com.dregs.sdk.exception.NotFoundException when no escalation has this id
     * @throws IllegalArgumentException when the status is not one an escalation has
     */
    public Escalation update(long escalationId, String status) {
        return client.run(Calls.escalationsUpdate(escalationId, status));
    }

    /**
     * Returns how many escalations are open, acknowledged, and closed.
     *
     * @return the summary
     */
    public EscalationSummary summary() {
        return client.run(Calls.escalationsSummary());
    }
}
