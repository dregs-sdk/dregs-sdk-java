package com.dregs.sdk;

import com.dregs.sdk.model.Escalation;
import com.dregs.sdk.model.EscalationSummary;
import com.dregs.sdk.model.Page;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The async twin of {@link Escalations}.
 *
 * <p>Reach one through {@link AsyncDregs#escalations()}. Each method does what the synchronous one
 * documents; the failures it would throw arrive as the future's cause instead.
 */
public final class AsyncEscalations {

    private final AsyncDregs client;

    AsyncEscalations(AsyncDregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of escalations. See {@link Escalations#list()}.
     *
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Escalation>> list() {
        return list(null);
    }

    /**
     * Returns one page of escalations. See {@link Escalations#list(EscalationListParams)}.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Escalation>> list(EscalationListParams params) {
        ListCall<Escalation> call = Calls.escalationsList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every escalation. See {@link #listAll(EscalationListParams)}.
     *
     * @return a future carrying the escalations
     */
    public CompletableFuture<List<Escalation>> listAll() {
        return listAll(null);
    }

    /**
     * Every escalation that matches, from every page.
     *
     * <p>The pages are fetched one after another and the future completes with all of them.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return a future carrying the escalations
     */
    public CompletableFuture<List<Escalation>> listAll(EscalationListParams params) {
        return client.walk(Calls.escalationsList(params));
    }

    /**
     * Returns the escalation with this id.
     *
     * @param escalationId the escalation's id
     * @return a future carrying the escalation
     */
    public CompletableFuture<Escalation> get(long escalationId) {
        return client.run(Calls.escalationsGet(escalationId));
    }

    /**
     * Moves the escalation to a status, and returns it.
     *
     * @param escalationId the escalation's id
     * @param status {@code "open"}, {@code "acknowledged"}, or {@code "closed"}, in either case
     * @return a future carrying the escalation, as it is now
     * @throws IllegalArgumentException when the status is not one an escalation has, thrown before
     *     any future is created
     */
    public CompletableFuture<Escalation> update(long escalationId, String status) {
        return client.run(Calls.escalationsUpdate(escalationId, status));
    }

    /**
     * Returns how many escalations are open, acknowledged, and closed.
     *
     * @return a future carrying the summary
     */
    public CompletableFuture<EscalationSummary> summary() {
        return client.run(Calls.escalationsSummary());
    }
}
