package com.dregs.sdk;

import com.dregs.sdk.model.Analysis;
import com.dregs.sdk.model.Identity;
import com.dregs.sdk.model.Scores;
import java.util.concurrent.CompletableFuture;

/**
 * The async twin of {@link Identities}.
 *
 * <p>Reach one through {@link AsyncDregs#identities()}. Each method does what the synchronous one
 * documents; the failures it would throw arrive as the future's cause instead.
 */
public final class AsyncIdentities {

    private final AsyncDregs client;

    AsyncIdentities(AsyncDregs client) {
        this.client = client;
    }

    /**
     * Returns the identity, with its current scores, badges, and attributes.
     *
     * @param identityId your own id for the user
     * @return a future carrying the identity, failing with
     *     {@link com.dregs.sdk.exception.NotFoundException} when Dregs has never seen it
     */
    public CompletableFuture<Identity> get(String identityId) {
        return client.request("GET", IdentityPaths.of(identityId), null)
                .thenApply(payload -> Identity.fromApi(Json.asObject(payload)));
    }

    /**
     * Returns the four current category scores.
     *
     * @param identityId your own id for the user
     * @return a future carrying the scores, which are empty for an identity nothing has scored yet
     */
    public CompletableFuture<Scores> scores(String identityId) {
        return client.request("GET", IdentityPaths.of(identityId, "/scores"), null)
                .thenApply(payload -> Scores.fromApi(Json.asObjects(payload)));
    }

    /**
     * Returns the most recent analysis cycle, with the observations behind each score.
     *
     * @param identityId your own id for the user
     * @return a future carrying the analysis, failing with
     *     {@link com.dregs.sdk.exception.NotFoundException} until the identity has been analyzed
     */
    public CompletableFuture<Analysis> analysis(String identityId) {
        return client.request("GET", IdentityPaths.of(identityId, "/analysis"), null)
                .thenApply(payload -> Analysis.fromApi(Json.asObject(payload)));
    }

    /**
     * Queues a re-analysis of the identity.
     *
     * @param identityId your own id for the user
     * @return a future that completes when the job is queued, not when it has run
     */
    public CompletableFuture<Void> analyze(String identityId) {
        return client.request("POST", IdentityPaths.of(identityId, "/actions/analyze"), null)
                .thenApply(payload -> null);
    }
}
