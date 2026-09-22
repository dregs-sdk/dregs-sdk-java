package com.dregs;

import java.util.concurrent.ThreadLocalRandom;

/**
 * How long to wait before the next attempt.
 *
 * <p>Package-private, and swapped out in tests so the retry suite does not spend its time asleep.
 */
@FunctionalInterface
interface RetryBackoff {

    /** The longest the SDK waits on its own, whatever the exponent works out to. */
    double MAX_JITTER_SECONDS = 8.0;

    /** The longest a server's {@code Retry-After} is honoured before it is capped. */
    double MAX_RETRY_AFTER_SECONDS = 60.0;

    /**
     * Exponential backoff with full jitter, or the server's own answer when it gave one.
     *
     * <p>Full jitter rather than a fixed schedule because a fleet of workers that all hit the
     * ingestion limit in the same second would otherwise retry in lockstep and hit it again.
     */
    RetryBackoff DEFAULT = (attempt, retryAfterSeconds) -> {
        if (retryAfterSeconds != null && retryAfterSeconds >= 0) {
            return (long) (Math.min(retryAfterSeconds, MAX_RETRY_AFTER_SECONDS) * 1000);
        }

        double ceiling = Math.min(0.5 * Math.pow(2, attempt), MAX_JITTER_SECONDS);

        return (long) (ThreadLocalRandom.current().nextDouble(0, ceiling) * 1000);
    };

    /**
     * Returns how many milliseconds to wait before attempt {@code attempt + 1}.
     *
     * @param attempt the zero-based number of the attempt that just failed
     * @param retryAfterSeconds the {@code Retry-After} the server sent, or null
     * @return milliseconds to wait
     */
    long millis(int attempt, Double retryAfterSeconds);
}
