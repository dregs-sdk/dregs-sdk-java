package com.dregs.model;

import java.util.Map;

/**
 * The outcome of a track call.
 *
 * @param status the status Dregs reported, normally {@code "success"}
 * @param id the event's identifier, either the one you supplied or one Dregs generated; it is
 *     {@code null} when the event was not recorded
 * @param fingerprint the device fingerprint Dregs resolved, for events that carried a device
 *     signature; server-side events do not, so this is normally {@code null}
 * @param raw the response body as received, so a field Dregs adds after this release is still
 *     reachable
 */
public record TrackResult(String status, String id, String fingerprint, Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public TrackResult {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Whether Dregs recorded the event.
     *
     * <p>This is {@code false} for the handful of rejections Dregs answers quietly, rather than
     * naming the check that failed: an event from an origin the credential does not allow, or one
     * carrying a malformed device signature or an unusable event id. Ingestion failures that are
     * yours to act on (a bad request, an unknown key, an exhausted quota, a rate limit) throw
     * instead of landing here.
     *
     * @return true when the event was recorded
     */
    public boolean accepted() {
        return id != null;
    }

    /**
     * Builds a result from a parsed {@code POST /api/events} response.
     *
     * @param payload the parsed response body
     * @return the result
     */
    public static TrackResult fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new TrackResult(
                ApiData.string(body, "status"),
                ApiData.string(body, "id"),
                ApiData.string(body, "fingerprint"),
                body);
    }
}
