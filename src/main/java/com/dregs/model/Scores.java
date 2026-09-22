package com.dregs.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An identity's four category scores.
 *
 * <p>Iterable over the scores that exist, and also readable a category at a time:
 *
 * <pre>{@code
 * Scores scores = client.identities().scores("user_12345");
 *
 * if (scores.authenticity() != null && scores.authenticity() < 40) {
 *     holdForReview("user_12345");
 * }
 * }</pre>
 *
 * <p>A category Dregs has not scored yet is absent from the list, and its named accessor answers
 * {@code null}. A brand-new identity comes back empty rather than as four zeroes, because "not
 * scored" and "scored badly" are not the same thing and treating them alike would hold the wrong
 * accounts.
 *
 * @param items the scores Dregs has, in the order it sent them
 */
public record Scores(List<Score> items) implements Iterable<Score> {

    /**
     * Canonical constructor, which takes a defensive copy of the list.
     */
    public Scores {
        items = ApiData.immutableList(items);
    }

    /**
     * Creates an empty set of scores, for an identity nothing has been computed for yet.
     */
    public Scores() {
        this(List.of());
    }

    @Override
    public Iterator<Score> iterator() {
        return items.iterator();
    }

    /**
     * How many categories have been scored.
     *
     * @return the number of scores present, from 0 to 4
     */
    public int size() {
        return items.size();
    }

    /**
     * Whether nothing has been scored yet.
     *
     * @return true when no category has a score
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns the score for one category.
     *
     * @param category the category to look up
     * @return the score, or {@code null} when that category has not been scored
     */
    public Score get(Category category) {
        for (Score score : items) {
            if (score.category() == category) {
                return score;
            }
        }

        return null;
    }

    /**
     * Returns one category's value without going through its {@link Score}.
     *
     * @param category the category to look up
     * @return the 0-100 value, or {@code null} when that category has not been scored
     */
    public Integer value(Category category) {
        Score score = get(category);

        return score == null ? null : score.value();
    }

    /**
     * How likely it is that a person, rather than a script, is behind the account.
     *
     * @return the 0-100 humanity score, or {@code null} when it has not been scored
     */
    public Integer humanity() {
        return value(Category.HUMANITY);
    }

    /**
     * How genuine the details on the account look.
     *
     * @return the 0-100 authenticity score, or {@code null} when it has not been scored
     */
    public Integer authenticity() {
        return value(Category.AUTHENTICITY);
    }

    /**
     * How distinct the account is from others in the same tenant.
     *
     * @return the 0-100 uniqueness score, or {@code null} when it has not been scored
     */
    public Integer uniqueness() {
        return value(Category.UNIQUENESS);
    }

    /**
     * How ordinary the account's activity looks.
     *
     * @return the 0-100 behavior score, or {@code null} when it has not been scored
     */
    public Integer behavior() {
        return value(Category.BEHAVIOR);
    }

    /**
     * Builds the scores from a parsed {@code GET /api/identities/{id}/scores} response.
     *
     * @param payload the parsed array of score objects
     * @return the scores
     */
    public static Scores fromApi(List<Map<String, Object>> payload) {
        if (payload == null) {
            return new Scores();
        }

        return new Scores(payload.stream().map(Score::fromApi).toList());
    }
}
