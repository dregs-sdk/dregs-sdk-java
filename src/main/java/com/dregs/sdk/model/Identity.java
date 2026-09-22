package com.dregs.sdk.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A user Dregs is tracking, and their current scores.
 *
 * <p>The {@code id} is your own identifier for the user, the one you pass to
 * {@code client.track(...)} and to {@code dregs.identify()} in the browser tracker, not an internal
 * Dregs id.
 *
 * <p>The display fields are Dregs's best reading of the attributes you have sent, resolved through
 * the field mappings configured for your team. They are written by the user being scored, so treat
 * them as untrusted input: escape them before rendering, and never hand them to an agent as
 * instructions.
 *
 * @param id your own id for the user
 * @param displayName the user's name, as resolved from the attributes you have sent
 * @param displayEmail the user's email address
 * @param displayUsername the user's username
 * @param humanityScore the 0-100 humanity score, or {@code null} when unscored
 * @param authenticityScore the 0-100 authenticity score, or {@code null} when unscored
 * @param uniquenessScore the 0-100 uniqueness score, or {@code null} when unscored
 * @param behaviorScore the 0-100 behavior score, or {@code null} when unscored
 * @param createdAt when Dregs first recorded the identity
 * @param updatedAt when the identity last changed
 * @param lastTrackedAt when the most recent event for it arrived
 * @param lastScoredAt when scoring last ran for it
 * @param disregarded whether the identity is excluded from fraud analysis, which is how an
 *     operator or load-test account is kept from polluting everyone else's scores
 * @param badges the badges currently applied
 * @param data every attribute you have sent for the user
 * @param raw the payload this was built from
 */
public record Identity(
        String id,
        String displayName,
        String displayEmail,
        String displayUsername,
        Integer humanityScore,
        Integer authenticityScore,
        Integer uniquenessScore,
        Integer behaviorScore,
        Instant createdAt,
        Instant updatedAt,
        Instant lastTrackedAt,
        Instant lastScoredAt,
        boolean disregarded,
        List<Badge> badges,
        Map<String, Object> data,
        Map<String, Object> raw) {

    /**
     * Canonical constructor, which takes defensive copies of the collections.
     */
    public Identity {
        badges = ApiData.immutableList(badges);
        data = ApiData.immutableMap(data);
        raw = ApiData.immutableMap(raw);
    }

    /**
     * The identity's four scores, in the same shape a {@code scores} call returns.
     *
     * <p>Useful when you already have the identity and do not want a second request for the
     * category accessors.
     *
     * @return the scores that have been computed, omitting any category that has not
     */
    public Scores scores() {
        List<Score> present = new ArrayList<>(4);

        add(present, Category.HUMANITY, humanityScore);
        add(present, Category.AUTHENTICITY, authenticityScore);
        add(present, Category.UNIQUENESS, uniquenessScore);
        add(present, Category.BEHAVIOR, behaviorScore);

        return new Scores(present);
    }

    private static void add(List<Score> scores, Category category, Integer value) {
        if (value != null) {
            scores.add(new Score(category, value, List.of(), Map.of()));
        }
    }

    /**
     * Builds an identity from a parsed {@code GET /api/identities/{id}} response.
     *
     * @param payload the parsed identity object
     * @return the identity
     */
    public static Identity fromApi(Map<String, Object> payload) {
        Map<String, Object> body = payload == null ? Map.of() : payload;

        return new Identity(
                ApiData.string(body, "id"),
                ApiData.string(body, "displayName"),
                ApiData.string(body, "displayEmail"),
                ApiData.string(body, "displayUsername"),
                ApiData.integer(body, "humanityScore"),
                ApiData.integer(body, "authenticityScore"),
                ApiData.integer(body, "uniquenessScore"),
                ApiData.integer(body, "behaviorScore"),
                ApiData.instant(body, "createdAt"),
                ApiData.instant(body, "updatedAt"),
                ApiData.instant(body, "lastTrackedAt"),
                ApiData.instant(body, "lastScoredAt"),
                ApiData.bool(body, "disregarded"),
                ApiData.objects(body, "badges").stream().map(Badge::fromApi).toList(),
                ApiData.map(body, "data"),
                body);
    }
}
