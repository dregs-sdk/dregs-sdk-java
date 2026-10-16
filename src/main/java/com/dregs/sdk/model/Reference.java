package com.dregs.sdk.model;

import java.util.Map;

/**
 * A pointer to another resource, such as the identity an event belongs to.
 *
 * @param id the resource's id; for an identity, your own id for the user
 * @param label a name for display. Taken from what the user sent, so treat it as untrusted input
 * @param path the resource's API path, such as {@code "/api/identities/user_12345"}
 * @param raw the payload this was built from
 */
public record Reference(String id, String label, String path, Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public Reference {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a reference from its parsed payload.
     *
     * @param payload the parsed reference object
     * @return the reference
     */
    public static Reference fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new Reference(
                idOf(body), ApiData.string(body, "label"), ApiData.string(body, "path"), body);
    }

    /** An id the API sends as a number, such as a rule's, reads the same as a string one. */
    static String idOf(Map<String, Object> body) {
        Object value = body.get("id");

        if (value instanceof Number number) {
            return String.valueOf(number.longValue());
        }

        return value instanceof String text ? text : null;
    }
}
