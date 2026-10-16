package com.dregs.sdk;

import com.dregs.sdk.model.Analysis;
import com.dregs.sdk.model.Identity;
import com.dregs.sdk.model.IdentityReview;
import com.dregs.sdk.model.LinkedIdentity;
import com.dregs.sdk.model.Page;
import com.dregs.sdk.model.ScoreHistoryEntry;
import com.dregs.sdk.model.Scores;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The async twin of {@link Identities}.
 *
 * <p>Reach one through {@link AsyncDregs#identities()}. Each method does what the synchronous one
 * documents; the failures it would throw arrive as the future's cause instead. An invalid argument
 * is still thrown straight away, before any future is created.
 */
public final class AsyncIdentities {

    private final AsyncDregs client;

    AsyncIdentities(AsyncDregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of identities. See {@link Identities#list()}.
     *
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Identity>> list() {
        return list(null);
    }

    /**
     * Returns one page of identities. See {@link Identities#list(IdentityListParams)}.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Identity>> list(IdentityListParams params) {
        ListCall<Identity> call = Calls.identitiesList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every identity. See {@link #listAll(IdentityListParams)}.
     *
     * @return a future carrying the identities
     */
    public CompletableFuture<List<Identity>> listAll() {
        return listAll(null);
    }

    /**
     * Every identity that matches, from every page.
     *
     * <p>The pages are fetched one after another and the future completes with all of them, so
     * for a result set too large to hold in memory, page with {@link #list(IdentityListParams)}
     * instead.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return a future carrying the identities
     */
    public CompletableFuture<List<Identity>> listAll(IdentityListParams params) {
        return client.walk(Calls.identitiesList(params));
    }

    /**
     * Returns the identity, with its current scores, badges, and attributes.
     *
     * @param identityId your own id for the user
     * @return a future carrying the identity, failing with
     *     {@link com.dregs.sdk.exception.NotFoundException} when Dregs has never seen it
     */
    public CompletableFuture<Identity> get(String identityId) {
        return client.run(Calls.identitiesGet(identityId));
    }

    /**
     * Returns the four current category scores.
     *
     * @param identityId your own id for the user
     * @return a future carrying the scores, which are empty for an identity nothing has scored yet
     */
    public CompletableFuture<Scores> scores(String identityId) {
        return client.run(Calls.identitiesScores(identityId));
    }

    /**
     * Returns the most recent analysis cycle, with the observations behind each score.
     *
     * @param identityId your own id for the user
     * @return a future carrying the analysis, failing with
     *     {@link com.dregs.sdk.exception.NotFoundException} until the identity has been analyzed
     */
    public CompletableFuture<Analysis> analysis(String identityId) {
        return client.run(Calls.identitiesAnalysis(identityId));
    }

    /**
     * Returns a past analysis cycle. See {@link Identities#analysis(String, long)}.
     *
     * @param identityId your own id for the user
     * @param cycleId the cycle, from {@link ScoreHistoryEntry#cycleId()}
     * @return a future carrying the analysis
     */
    public CompletableFuture<Analysis> analysis(String identityId, long cycleId) {
        return client.run(Calls.identitiesAnalysis(identityId, cycleId));
    }

    /**
     * Returns the scores of past analysis cycles, newest first.
     *
     * @param identityId your own id for the user
     * @return a future carrying the history
     */
    public CompletableFuture<List<ScoreHistoryEntry>> history(String identityId) {
        return client.run(Calls.identitiesHistory(identityId, null));
    }

    /**
     * Returns the scores of past analysis cycles, newest first.
     *
     * @param identityId your own id for the user
     * @param limit the most cycles to return
     * @return a future carrying the history
     */
    public CompletableFuture<List<ScoreHistoryEntry>> history(String identityId, int limit) {
        return client.run(Calls.identitiesHistory(identityId, limit));
    }

    /**
     * Returns the other identities Dregs believes are connected to this one.
     *
     * @param identityId your own id for the user
     * @return a future carrying the links
     */
    public CompletableFuture<List<LinkedIdentity>> links(String identityId) {
        return client.run(Calls.identitiesLinks(identityId));
    }

    /**
     * Queues a re-analysis of the identity.
     *
     * @param identityId your own id for the user
     * @return a future that completes when the job is queued, not when it has run
     */
    public CompletableFuture<Void> analyze(String identityId) {
        return client.run(Calls.identitiesAnalyze(identityId));
    }

    /**
     * Updates an identity and returns it. See {@link Identities#update(String, IdentityUpdate)}.
     *
     * @param identityId your own id for the user
     * @param update what to change
     * @return a future carrying the identity, as it is now
     */
    public CompletableFuture<Identity> update(String identityId, IdentityUpdate update) {
        return client.run(Calls.identitiesUpdate(identityId, update));
    }

    /**
     * Returns the AI reviews requested for this identity, newest first.
     *
     * @param identityId your own id for the user
     * @return a future carrying the reviews
     */
    public CompletableFuture<List<IdentityReview>> reviews(String identityId) {
        return client.run(Calls.identitiesReviews(identityId));
    }

    /**
     * Asks Dregs's AI reviewer to look over the identity's analysis.
     *
     * @param identityId your own id for the user
     * @return a future carrying the review, which is {@code "PENDING"} until it has run
     */
    public CompletableFuture<IdentityReview> review(String identityId) {
        return review(identityId, null);
    }

    /**
     * Asks Dregs's AI reviewer to look over the identity's analysis, with a note. See
     * {@link Identities#review(String, String)}.
     *
     * @param identityId your own id for the user
     * @param feedback what you think the analysis got wrong, or {@code null} for none
     * @return a future carrying the review, which is {@code "PENDING"} until it has run
     */
    public CompletableFuture<IdentityReview> review(String identityId, String feedback) {
        return client.run(Calls.identitiesReview(identityId, feedback));
    }
}
