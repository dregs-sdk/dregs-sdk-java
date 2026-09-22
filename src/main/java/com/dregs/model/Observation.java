package com.dregs.model;

import java.util.Map;

/**
 * One analyzer's finding, and the reasoning behind a slice of a score.
 *
 * <p>Observations are the evidence. A score says an identity looks wrong; its observations say
 * which analyzers thought so, how strongly, and on what counts.
 *
 * @param category the category the observation contributes to, or {@code null} for one this SDK
 *     release does not recognize
 * @param id the analyzer's identifier, such as {@code "humanity.user-agent"}
 * @param label a human-readable name for the analyzer
 * @param explanation a sentence describing what the analyzer found
 * @param value 0.0 for entirely suspicious, 1.0 for entirely legitimate
 * @param confidence how sure the analyzer is, from 0.0 to 1.0
 * @param weight how heavily this observation counts toward the category score
 * @param metadata the counts and details behind the finding
 * @param raw the payload this was built from
 */
public record Observation(
        Category category,
        String id,
        String label,
        String explanation,
        Double value,
        Double confidence,
        Double weight,
        Map<String, Object> metadata,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the maps.
     */
    public Observation {
        metadata = ApiData.immutableMap(metadata);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds an observation from its parsed payload.
     *
     * @param payload the parsed observation object
     * @return the observation
     */
    public static Observation fromApi(Map<String, Object> payload) {
        return new Observation(
                Category.fromApi(ApiData.string(payload, "category")),
                ApiData.string(payload, "id"),
                ApiData.string(payload, "label"),
                ApiData.string(payload, "explanation"),
                ApiData.decimal(payload, "value"),
                ApiData.decimal(payload, "confidence"),
                ApiData.decimal(payload, "weight"),
                ApiData.map(payload, "metadata"),
                payload);
    }
}
