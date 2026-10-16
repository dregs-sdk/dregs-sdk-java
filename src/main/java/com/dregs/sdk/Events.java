package com.dregs.sdk;

import com.dregs.sdk.model.Event;
import com.dregs.sdk.model.Page;

/**
 * Reading the events Dregs has recorded. To record one, use {@link Dregs#track(TrackRequest)}.
 *
 * <p>Reach one through {@link Dregs#events()}.
 */
public final class Events {

    private final Dregs client;

    Events(Dregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of events, newest first.
     *
     * @return the page
     */
    public Page<Event> list() {
        return list(null);
    }

    /**
     * Returns one page of events.
     *
     * <p>Use {@link #listAll(EventListParams)} to walk every page.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return the page
     */
    public Page<Event> list(EventListParams params) {
        ListCall<Event> call = Calls.eventsList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every event, fetching pages as the iteration reaches them.
     *
     * @return the events
     */
    public Iterable<Event> listAll() {
        return listAll(null);
    }

    /**
     * Every event that matches, fetching pages as the iteration reaches them.
     *
     * <p>Nothing is fetched until the loop starts, and each new loop starts a fresh walk.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return the events
     */
    public Iterable<Event> listAll(EventListParams params) {
        return client.walk(Calls.eventsList(params));
    }

    /**
     * Returns the event with this id.
     *
     * @param eventId the event's id, either the one you sent or one Dregs generated
     * @return the event
     * @throws com.dregs.sdk.exception.NotFoundException when no event has this id
     * @throws IllegalArgumentException when the id is empty
     */
    public Event get(String eventId) {
        return client.run(Calls.eventsGet(eventId));
    }
}
