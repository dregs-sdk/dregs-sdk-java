package com.dregs.sdk;

import java.util.Map;

/**
 * A group the user is acting in, such as their organization or team.
 *
 * <p>Add groups to an event with {@link TrackRequest.Builder#group(String, String, Map)}. Dregs
 * records each group and makes the event's identity a member of it.
 *
 * @param type your own name for the kind of group, such as {@code "organization"} or
 *     {@code "team"}; Dregs normalizes it to lower_snake_case
 * @param id your own id for the group
 * @param data attributes of the group, which Dregs merges into it; empty when none was set
 */
public record TrackGroup(String type, String id, Map<String, Object> data) {

    /** The type a group takes when none is given. */
    public static final String DEFAULT_TYPE = "organization";

    /**
     * Canonical constructor, which defaults a missing type to {@value #DEFAULT_TYPE} and takes a
     * defensive copy of the data.
     */
    public TrackGroup {
        type = type == null || type.isEmpty() ? DEFAULT_TYPE : type;
        data = TrackRequest.copyOf(data);
    }
}
