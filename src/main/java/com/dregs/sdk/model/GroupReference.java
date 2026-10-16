package com.dregs.sdk.model;

import java.util.Map;

/**
 * A pointer to a group, as it appears on an event.
 *
 * @param type the kind of group, such as {@code "organization"} or {@code "team"}
 * @param id your own id for the group
 * @param label a name for display, taken from the group's attributes
 * @param path the group's API path, such as {@code "/api/groups/team/team_42"}
 * @param raw the payload this was built from
 */
public record GroupReference(
        String type, String id, String label, String path, Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public GroupReference {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a group reference from its parsed payload.
     *
     * @param payload the parsed group reference object
     * @return the group reference
     */
    public static GroupReference fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new GroupReference(
                ApiData.string(body, "type"),
                ApiData.string(body, "id"),
                ApiData.string(body, "label"),
                ApiData.string(body, "path"),
                body);
    }
}
