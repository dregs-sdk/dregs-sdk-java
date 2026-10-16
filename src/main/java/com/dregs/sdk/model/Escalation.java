package com.dregs.sdk.model;

import java.time.Instant;
import java.util.Map;

/**
 * An escalation an escalation rule opened against an identity.
 *
 * @param id the escalation's id
 * @param status {@code "OPEN"}, {@code "ACKNOWLEDGED"}, or {@code "CLOSED"}
 * @param severity {@code "INFO"}, {@code "WARNING"}, or {@code "CRITICAL"}
 * @param identity the identity the escalation is about
 * @param rule the escalation rule that opened it
 * @param scores the identity's scores when the escalation opened, by category name
 * @param openedAt when it opened
 * @param acknowledgedAt when it was acknowledged, or {@code null}
 * @param closedAt when it was closed, or {@code null}
 * @param createdAt when Dregs recorded it
 * @param raw the payload this was built from
 */
public record Escalation(
        Long id,
        String status,
        String severity,
        Reference identity,
        Reference rule,
        Map<String, Object> scores,
        Instant openedAt,
        Instant acknowledgedAt,
        Instant closedAt,
        Instant createdAt,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the maps.
     */
    public Escalation {
        scores = ApiData.immutableMap(scores);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * Builds an escalation from a parsed {@code GET /api/escalations/{id}} response.
     *
     * @param payload the parsed escalation object
     * @return the escalation
     */
    public static Escalation fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;
        Map<String, Object> identity = ApiData.objectOrNull(body, "identity");
        Map<String, Object> rule = ApiData.objectOrNull(body, "escalationRule");

        return new Escalation(
                ApiData.longValue(body, "id"),
                ApiData.string(body, "status"),
                ApiData.string(body, "severity"),
                identity == null ? null : Reference.fromApi(identity),
                rule == null ? null : Reference.fromApi(rule),
                ApiData.map(body, "scoreSnapshot"),
                ApiData.instant(body, "openedAt"),
                ApiData.instant(body, "acknowledgedAt"),
                ApiData.instant(body, "closedAt"),
                ApiData.instant(body, "createdAt"),
                body);
    }
}
