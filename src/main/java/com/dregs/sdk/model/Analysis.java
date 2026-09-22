package com.dregs.sdk.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One analysis cycle: the scores an identity was given, and why.
 *
 * <p>This is the read to reach for when you need to show or log the reasoning behind a score. Each
 * of its scores carries the observations that produced it.
 *
 * @param id the cycle's identifier
 * @param identityId the identity that was analyzed
 * @param scores the category scores, each carrying its observations
 * @param eventCount how many events the cycle considered
 * @param deviceCount how many devices the cycle considered
 * @param durationMillis how long the cycle took
 * @param startedAt when the cycle began
 * @param finishedAt when the cycle finished
 * @param raw the payload this was built from
 */
public record Analysis(
        Long id,
        String identityId,
        Scores scores,
        Integer eventCount,
        Integer deviceCount,
        Long durationMillis,
        Instant startedAt,
        Instant finishedAt,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which defaults the scores and copies the raw payload.
     */
    public Analysis {
        scores = scores == null ? new Scores() : scores;
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Every observation from the cycle, across all categories.
     *
     * @return the observations, in category order
     */
    public List<Observation> observations() {
        List<Observation> all = new ArrayList<>();

        for (Score score : scores) {
            all.addAll(score.observations());
        }

        return List.copyOf(all);
    }

    /**
     * Builds an analysis from a parsed {@code GET /api/identities/{id}/analysis} response.
     *
     * @param payload the parsed analysis object
     * @return the analysis
     */
    public static Analysis fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new Analysis(
                ApiData.longValue(body, "id"),
                ApiData.string(body, "identityId"),
                Scores.fromApi(ApiData.objects(body, "scores")),
                ApiData.integer(body, "eventCount"),
                ApiData.integer(body, "deviceCount"),
                ApiData.longValue(body, "durationMillis"),
                ApiData.instant(body, "startedAt"),
                ApiData.instant(body, "finishedAt"),
                body);
    }
}
