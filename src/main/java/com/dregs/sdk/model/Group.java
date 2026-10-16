package com.dregs.sdk.model;

import java.time.Instant;
import java.util.Map;

/**
 * An organization, team, workspace, or whatever your application groups users into.
 *
 * <p>A group is identified by its type and id together, both your own. Dregs normalizes the type to
 * lower_snake_case.
 *
 * @param type the kind of group, such as {@code "organization"} or {@code "team"}
 * @param id your own id for the group
 * @param displayName a name for display, taken from the group's attributes
 * @param identityCount how many identities are members
 * @param createdAt when Dregs first recorded the group
 * @param updatedAt when the group last changed
 * @param lastTrackedAt when the most recent event sent with it arrived
 * @param data the group's attributes. Empty on the groups of a list call, which leaves them out
 * @param raw the payload this was built from
 */
public record Group(
        String type,
        String id,
        String displayName,
        Integer identityCount,
        Instant createdAt,
        Instant updatedAt,
        Instant lastTrackedAt,
        Map<String, Object> data,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the maps.
     */
    public Group {
        data = ApiData.immutableMap(data);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a group from a parsed {@code GET /api/groups/{type}/{id}} response.
     *
     * @param payload the parsed group object
     * @return the group
     */
    public static Group fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new Group(
                ApiData.string(body, "type"),
                ApiData.string(body, "id"),
                ApiData.string(body, "displayName"),
                ApiData.integer(body, "identityCount"),
                ApiData.instant(body, "createdAt"),
                ApiData.instant(body, "updatedAt"),
                ApiData.instant(body, "lastTrackedAt"),
                ApiData.map(body, "data"),
                body);
    }
}
