package com.dregs.sdk.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * An event Dregs recorded.
 *
 * <p>The data is what the browser tracker or your backend sent, so it is written by the user being
 * scored as much as by you. Treat it as untrusted input.
 *
 * @param id the event's id, either the one you sent or one Dregs generated
 * @param timestamp when the event happened
 * @param identity the identity the event belongs to, or {@code null} for an anonymous event
 * @param groups the groups the event was sent with
 * @param fingerprint the device fingerprint, for events that carried a device signature
 * @param session the browser session, for events from the tracker
 * @param ip the address the event came from
 * @param url the page URL, for events from the tracker
 * @param source where the event came from, such as {@code "java-sdk"}
 * @param name your name for the event, such as {@code "user.signup"}
 * @param canonicalType the Dregs event type your name is mapped to, or {@code null} when unmapped
 * @param data the event's attributes, with sensitive values redacted
 * @param raw the payload this was built from
 */
public record Event(
        String id,
        Instant timestamp,
        Reference identity,
        List<GroupReference> groups,
        String fingerprint,
        String session,
        String ip,
        String url,
        String source,
        String name,
        String canonicalType,
        Map<String, Object> data,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the collections.
     */
    public Event {
        groups = ApiData.immutableList(groups);
        data = ApiData.immutableMap(data);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds an event from a parsed {@code GET /api/events/{id}} response.
     *
     * @param payload the parsed event object
     * @return the event
     */
    public static Event fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;
        Map<String, Object> identity = ApiData.objectOrNull(body, "identity");

        return new Event(
                ApiData.string(body, "id"),
                ApiData.instant(body, "timestamp"),
                identity == null ? null : Reference.fromApi(identity),
                ApiData.objects(body, "groups").stream().map(GroupReference::fromApi).toList(),
                ApiData.string(body, "fingerprint"),
                ApiData.string(body, "session"),
                ApiData.string(body, "ip"),
                ApiData.string(body, "url"),
                ApiData.string(body, "eventSource"),
                ApiData.string(body, "eventName"),
                ApiData.string(body, "canonicalType"),
                ApiData.map(body, "eventData"),
                body);
    }
}
