package com.dregs.sdk.model;

import java.time.Instant;
import java.util.Map;

/**
 * Another identity Dregs believes is connected to this one, such as by a shared device.
 *
 * @param identityId the linked identity's id
 * @param displayName the linked identity's display name. Written by that user, so treat it as
 *     untrusted input
 * @param type how they are linked, such as {@code "SAME_DEVICE"} or {@code "SHARED_IP"}
 * @param value how strongly they are linked, from 0.0 to 1.0
 * @param confidence how sure Dregs is of the link, from 0.0 to 1.0
 * @param explanation a sentence describing the link
 * @param metadata the details behind it
 * @param createdAt when Dregs found the link
 * @param raw the payload this was built from
 */
public record LinkedIdentity(
        String identityId,
        String displayName,
        String type,
        Double value,
        Double confidence,
        String explanation,
        Map<String, Object> metadata,
        Instant createdAt,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the maps.
     */
    public LinkedIdentity {
        metadata = ApiData.immutableMap(metadata);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a link from its parsed payload.
     *
     * @param payload the parsed link object
     * @return the link
     */
    public static LinkedIdentity fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new LinkedIdentity(
                ApiData.string(body, "targetIdentityId"),
                ApiData.string(body, "targetDisplayName"),
                ApiData.string(body, "type"),
                ApiData.decimal(body, "value"),
                ApiData.decimal(body, "confidence"),
                ApiData.string(body, "explanation"),
                ApiData.map(body, "metadata"),
                ApiData.instant(body, "createdAt"),
                body);
    }
}
