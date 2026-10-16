package com.dregs.sdk;

import com.dregs.sdk.model.Event;
import com.dregs.sdk.model.Page;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The async twin of {@link Events}.
 *
 * <p>Reach one through {@link AsyncDregs#events()}. Each method does what the synchronous one
 * documents; the failures it would throw arrive as the future's cause instead.
 */
public final class AsyncEvents {

    private final AsyncDregs client;

    AsyncEvents(AsyncDregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of events. See {@link Events#list()}.
     *
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Event>> list() {
        return list(null);
    }

    /**
     * Returns one page of events. See {@link Events#list(EventListParams)}.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Event>> list(EventListParams params) {
        ListCall<Event> call = Calls.eventsList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every event. See {@link #listAll(EventListParams)}.
     *
     * @return a future carrying the events
     */
    public CompletableFuture<List<Event>> listAll() {
        return listAll(null);
    }

    /**
     * Every event that matches, from every page.
     *
     * <p>The pages are fetched one after another and the future completes with all of them, so
     * for a result set too large to hold in memory, page with {@link #list(EventListParams)}
     * instead.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return a future carrying the events
     */
    public CompletableFuture<List<Event>> listAll(EventListParams params) {
        return client.walk(Calls.eventsList(params));
    }

    /**
     * Returns the event with this id.
     *
     * @param eventId the event's id
     * @return a future carrying the event
     */
    public CompletableFuture<Event> get(String eventId) {
        return client.run(Calls.eventsGet(eventId));
    }
}
