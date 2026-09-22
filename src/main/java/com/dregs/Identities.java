package com.dregs;

import com.dregs.model.Analysis;
import com.dregs.model.Identity;
import com.dregs.model.Scores;

/**
 * Reading identities and their scores.
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
     * Returns the identity, with its current scores, badges, and attributes.
     *
     * @param identityId your own id for the user
     * @return the identity
     * @throws com.dregs.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public Identity get(String identityId) {
        return Identity.fromApi(Json.asObject(client.request("GET", IdentityPaths.of(identityId), null)));
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
     * @throws com.dregs.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public Scores scores(String identityId) {
        return Scores.fromApi(
                Json.asObjects(client.request("GET", IdentityPaths.of(identityId, "/scores"), null)));
    }

    /**
     * Returns the most recent analysis cycle, with the observations behind each score.
     *
     * <p>Use this when you need to show or log <em>why</em> an identity scored the way it did.
     *
     * @param identityId your own id for the user
     * @return the analysis cycle
     * @throws com.dregs.exception.NotFoundException when the identity is unknown, or it has not
     *     been analyzed yet
     * @throws IllegalArgumentException when the id is empty
     */
    public Analysis analysis(String identityId) {
        return Analysis.fromApi(
                Json.asObject(client.request("GET", IdentityPaths.of(identityId, "/analysis"), null)));
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
     * @throws com.dregs.exception.NotFoundException when Dregs has never seen this identity
     * @throws IllegalArgumentException when the id is empty
     */
    public void analyze(String identityId) {
        client.request("POST", IdentityPaths.of(identityId, "/actions/analyze"), null);
    }
}
