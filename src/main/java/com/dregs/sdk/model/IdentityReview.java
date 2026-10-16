package com.dregs.sdk.model;

import java.time.Instant;
import java.util.Map;

/**
 * A review of an identity's analysis, written by Dregs's AI reviewer.
 *
 * @param id the review's id
 * @param identityId the identity reviewed
 * @param status {@code "PENDING"} until the review has run, then {@code "FINISHED"} or
 *     {@code "FAILED"}
 * @param feedback the note you sent with the request, if any
 * @param summary the reviewer's findings, once it has run
 * @param errorMessage why the review failed, when it did
 * @param createdAt when the review was requested
 * @param completedAt when the review finished
 * @param raw the payload this was built from
 */
public record IdentityReview(
        Long id,
        String identityId,
        String status,
        String feedback,
        String summary,
        String errorMessage,
        Instant createdAt,
        Instant completedAt,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public IdentityReview {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a review from its parsed payload.
     *
     * @param payload the parsed review object
     * @return the review
     */
    public static IdentityReview fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new IdentityReview(
                ApiData.longValue(body, "id"),
                ApiData.string(body, "identityId"),
                ApiData.string(body, "status"),
                ApiData.string(body, "userFeedback"),
                ApiData.string(body, "summary"),
                ApiData.string(body, "errorMessage"),
                ApiData.instant(body, "createdAt"),
                ApiData.instant(body, "completedAt"),
                body);
    }
}
