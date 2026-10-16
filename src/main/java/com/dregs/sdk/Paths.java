package com.dregs.sdk;

/**
 * Building API paths, with every caller-supplied segment escaped.
 *
 * <p>An empty id is refused here, before any request is made: sent as is, it would turn
 * {@code /identities/{id}} into the list endpoint and come back as a confusing success.
 */
final class Paths {

    private Paths() {
    }

    /**
     * One escaped path segment.
     *
     * @param value the caller's id
     * @param what how to name it in the error, such as "An identity id"
     * @throws IllegalArgumentException when the value is null or empty
     */
    static String segment(String value, String what) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(what + " is required.");
        }

        return AbstractDregsClient.encodeSegment(value);
    }

    static String identity(String identityId) {
        return identity(identityId, "");
    }

    static String identity(String identityId, String suffix) {
        return "/identities/" + segment(identityId, "An identity id") + suffix;
    }

    static String group(String type, String groupId) {
        return group(type, groupId, "");
    }

    static String group(String type, String groupId, String suffix) {
        return "/groups/" + segment(type, "A group type") + "/" + segment(groupId, "A group id")
                + suffix;
    }

    static String event(String eventId) {
        return "/events/" + segment(eventId, "An event id");
    }

    static String device(String fingerprint) {
        return "/devices/" + segment(fingerprint, "A device fingerprint");
    }

    static String escalation(long escalationId) {
        return "/escalations/" + escalationId;
    }
}
