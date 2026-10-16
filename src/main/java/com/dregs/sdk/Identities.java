package com.dregs.sdk;

import com.dregs.sdk.model.Analysis;
import com.dregs.sdk.model.Identity;
import com.dregs.sdk.model.IdentityReview;
import com.dregs.sdk.model.LinkedIdentity;
import com.dregs.sdk.model.Page;
import com.dregs.sdk.model.ScoreHistoryEntry;
import com.dregs.sdk.model.Scores;
import java.util.List;

/**
 * Reading identities, their scores, and their analysis.
 *
 * <p>Reach one through {@link Dregs#identities()}. These calls are thin: they name the endpoint,
 * then hand the response to a model. The transport, the retries, and the error mapping all live on
 * the client.
 */
public final class Identities {

    private final Dregs client;

    Identities(Dregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of identities, most recently updated first.
     *
     * @return the page
     */
    public Page<Identity> list() {
        return list(null);
    }

    /**
     * Returns one page of identities.
     *
     * <p>Use {@link #listAll(IdentityListParams)} to walk every page.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return the page
     */
    public Page<Identity> list(IdentityListParams params) {
        ListCall<Identity> call = Calls.identitiesList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every identity, fetching pages as the iteration reaches them.
     *
     * @return the identities
     */
    public Iterable<Identity> listAll() {
        return listAll(null);
    }

    /**
     * Every identity that matches, fetching pages as the iteration reaches them.
     *
     * <pre>{@code
     * for (Identity identity : client.identities().listAll(IdentityListParams.builder()
     *         .group("org_678")
     *         .build())) {
     *     ...
     * }
     * }</pre>
     *
     * <p>Nothing is fetched until the loop starts, and each new loop starts a fresh walk.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return the identities
     */
    public Iterable<Identity> listAll(IdentityListParams params) {
        return client.walk(Calls.identitiesList(params));
    }

    /**
     * Returns the identity, with its current scores, badges, and attributes.
     *
     * @param identityId your own id for the user
     * @return the identity
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public Identity get(String identityId) {
        return client.run(Calls.identitiesGet(identityId));
    }

    /**
     * Returns the four current category scores.
     *
     * <p>This is the cheap read and the one most integrations want. It reports the scores Dregs has
     * already computed without triggering any work. For the observations behind them, use
     * {@link #analysis(String)}.
     *
     * <p>A category that has not been scored yet is absent, so a brand-new identity comes back
     * empty.
     *
     * @param identityId your own id for the user
     * @return the scores
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public Scores scores(String identityId) {
        return client.run(Calls.identitiesScores(identityId));
    }

    /**
     * Returns the most recent analysis cycle, with the observations behind each score.
     *
     * <p>Use this when you need to show or log <em>why</em> an identity scored the way it did.
     *
     * @param identityId your own id for the user
     * @return the analysis cycle
     * @throws com.dregs.sdk.exception.NotFoundException when the identity is unknown, or it has not
     *     been analyzed yet
     * @throws IllegalArgumentException when the id is empty
     */
    public Analysis analysis(String identityId) {
        return client.run(Calls.identitiesAnalysis(identityId));
    }

    /**
     * Returns a past analysis cycle, with the observations behind each score.
     *
     * @param identityId your own id for the user
     * @param cycleId the cycle, from {@link ScoreHistoryEntry#cycleId()}
     * @return the analysis cycle
     * @throws com.dregs.sdk.exception.NotFoundException when the identity or the cycle is unknown
     * @throws IllegalArgumentException when the id is empty
     */
    public Analysis analysis(String identityId, long cycleId) {
        return client.run(Calls.identitiesAnalysis(identityId, cycleId));
    }

    /**
     * Returns the scores of past analysis cycles, newest first, as many as the API's default of
     * 100.
     *
     * @param identityId your own id for the user
     * @return the history
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public List<ScoreHistoryEntry> history(String identityId) {
        return client.run(Calls.identitiesHistory(identityId, null));
    }

    /**
     * Returns the scores of past analysis cycles, newest first.
     *
     * @param identityId your own id for the user
     * @param limit the most cycles to return
     * @return the history
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public List<ScoreHistoryEntry> history(String identityId, int limit) {
        return client.run(Calls.identitiesHistory(identityId, limit));
    }

    /**
     * Returns the other identities Dregs believes are connected to this one, such as by a shared
     * device.
     *
     * @param identityId your own id for the user
     * @return the links
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public List<LinkedIdentity> links(String identityId) {
        return client.run(Calls.identitiesLinks(identityId));
    }

    /**
     * Queues a re-analysis of the identity.
     *
     * <p>Scoring is asynchronous: this returns as soon as the job is queued, not when it has run.
     * Poll {@link #scores(String)} or watch for a webhook rather than expecting fresh scores on the
     * next line. Dregs rescores on its own as events arrive, so this is rarely needed outside a
     * support or backfill flow.
     *
     * @param identityId your own id for the user
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public void analyze(String identityId) {
        client.run(Calls.identitiesAnalyze(identityId));
    }

    /**
     * Updates an identity and returns it.
     *
     * <pre>{@code
     * client.identities().update("qa_bot_7", IdentityUpdate.builder().disregarded(true).build());
     * }</pre>
     *
     * @param identityId your own id for the user
     * @param update what to change
     * @return the identity, as it is now
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty or the update is null
     */
    public Identity update(String identityId, IdentityUpdate update) {
        return client.run(Calls.identitiesUpdate(identityId, update));
    }

    /**
     * Returns the AI reviews requested for this identity, newest first.
     *
     * @param identityId your own id for the user
     * @return the reviews
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public List<IdentityReview> reviews(String identityId) {
        return client.run(Calls.identitiesReviews(identityId));
    }

    /**
     * Asks Dregs's AI reviewer to look over the identity's analysis.
     *
     * @param identityId your own id for the user
     * @return the review, which is {@code "PENDING"} until it has run
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public IdentityReview review(String identityId) {
        return review(identityId, null);
    }

    /**
     * Asks Dregs's AI reviewer to look over the identity's analysis, with a note on what you think
     * it got wrong.
     *
     * <p>The review runs in the background, so the result comes back {@code "PENDING"}. Read it
     * again with {@link #reviews(String)} once it has finished.
     *
     * @param identityId your own id for the user
     * @param feedback what you think the analysis got wrong, such as "this is a real customer", or
     *     {@code null} for none
     * @return the review, which is {@code "PENDING"} until it has run
     * @throws com.dregs.sdk.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public IdentityReview review(String identityId, String feedback) {
        return client.run(Calls.identitiesReview(identityId, feedback));
    }
}
