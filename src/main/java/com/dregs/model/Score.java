package com.dregs.model;

import java.util.List;
import java.util.Map;

/**
 * One category's score.
 *
 * @param category the category scored, or {@code null} for one this SDK release does not recognize
 * @param value an integer from 0 (worst) to 100 (best)
 * @param observations the observations behind the score; this is empty on the result of a
 *     {@code scores} call, which reports the scores alone, and populated on an {@code analysis}
 * @param raw the payload this was built from
 */
public record Score(
        Category category, Integer value, List<Observation> observations, Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the collections.
     */
    public Score {
        observations = ApiData.immutableList(observations);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a score from its parsed payload.
     *
     * @param payload the parsed score object
     * @return the score
     */
    public static Score fromApi(Map<String, Object> payload) {
        return new Score(
                Category.fromApi(ApiData.string(payload, "category")),
                ApiData.integer(payload, "value"),
                ApiData.objects(payload, "observations").stream().map(Observation::fromApi).toList(),
                payload);
    }
}
