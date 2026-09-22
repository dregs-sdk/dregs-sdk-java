package com.dregs;

/** Building the {@code /identities/...} paths, with the id escaped. */
final class IdentityPaths {

    private IdentityPaths() {
    }

    static String of(String identityId) {
        return of(identityId, "");
    }

    static String of(String identityId, String suffix) {
        if (identityId == null || identityId.isEmpty()) {
            throw new IllegalArgumentException("An identity id is required.");
        }

        return "/identities/" + AbstractDregsClient.encodeSegment(identityId) + suffix;
    }
}
