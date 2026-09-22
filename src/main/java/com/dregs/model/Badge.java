package com.dregs.model;

import java.util.Map;

/**
 * A label Dregs applied to an identity, from an analyzer or a badge rule.
 *
 * <p>Badges are the durable verdicts: an analyzer badge such as "Account Takeover Suspected" is
 * attached while the observation behind it keeps firing and removed when it stops.
 *
 * @param slug the badge's identifier, such as {@code "behavior.account-takeover-signal"}
 * @param name the human-readable name shown in the dashboard
 * @param type the badge's kind, which the dashboard renders as a colour
 * @param explanation a sentence describing why it was applied
 * @param metadata the details behind it
 * @param raw the payload this was built from
 */
public record Badge(
        String slug,
        String name,
        String type,
        String explanation,
        Map<String, Object> metadata,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the maps.
     */
    public Badge {
        metadata = ApiData.immutableMap(metadata);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a badge from its parsed payload.
     *
     * @param payload the parsed badge object
     * @return the badge
     */
    public static Badge fromApi(Map<String, Object> payload) {
        return new Badge(
                ApiData.string(payload, "slug"),
                ApiData.string(payload, "name"),
                ApiData.string(payload, "type"),
                ApiData.string(payload, "explanation"),
                ApiData.map(payload, "metadata"),
                payload);
    }
}
