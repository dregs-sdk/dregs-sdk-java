package com.dregs.sdk.model;

import java.util.Map;

/**
 * How many escalations are in each status.
 *
 * @param open how many are open
 * @param acknowledged how many are acknowledged
 * @param closed how many are closed
 * @param raw the payload this was built from
 */
public record EscalationSummary(long open, long acknowledged, long closed, Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes a defensive copy of the raw payload.
     */
    public EscalationSummary {
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds a summary from a parsed {@code GET /api/escalations/summary} response.
     *
     * @param payload the parsed summary object
     * @return the summary
     */
    public static EscalationSummary fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new EscalationSummary(
                orZero(ApiData.longValue(body, "open")),
                orZero(ApiData.longValue(body, "acknowledged")),
                orZero(ApiData.longValue(body, "closed")),
                body);
    }

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }
}
