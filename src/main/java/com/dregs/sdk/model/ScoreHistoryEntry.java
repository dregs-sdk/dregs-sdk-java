package com.dregs.sdk.model;

import java.time.Instant;
import java.util.Map;

/**
 * The scores one past analysis cycle produced.
 *
 * @param cycleId the cycle's id, which {@code identities().analysis(identityId, cycleId)} accepts to
 *     fetch that cycle's observations
 * @param humanity the 0-100 humanity score, or {@code null} when the cycle did not score it
 * @param authenticity the 0-100 authenticity score, or {@code null} when the cycle did not score it
 * @param uniqueness the 0-100 uniqueness score, or {@code null} when the cycle did not score it
 * @param behavior the 0-100 behavior score, or {@code null} when the cycle did not score it
 * @param eventCount how many events the cycle considered
 * @param deviceCount how many devices the cycle considered
 * @param finishedAt when the cycle finished
 * @param raw the payload this was built from
 */
public record ScoreHistoryEntry(
        Long cycleId,
        Integer humanity,
        Integer authenticity,
        Integer uniqueness,
        Integer behavior,
        Integer eventCount,
        Integer deviceCount,
        Instant finishedAt,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public ScoreHistoryEntry {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a history entry from its parsed payload.
     *
     * @param payload the parsed history entry
     * @return the entry
     */
    public static ScoreHistoryEntry fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new ScoreHistoryEntry(
                ApiData.longValue(body, "id"),
                ApiData.integer(body, "humanityScore"),
                ApiData.integer(body, "authenticityScore"),
                ApiData.integer(body, "uniquenessScore"),
                ApiData.integer(body, "behaviorScore"),
                ApiData.integer(body, "eventCount"),
                ApiData.integer(body, "deviceCount"),
                ApiData.instant(body, "finishedAt"),
                body);
    }
}
