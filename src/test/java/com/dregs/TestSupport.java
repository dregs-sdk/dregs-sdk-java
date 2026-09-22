package com.dregs;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Shared fixtures: the fake credential, the environment seam, and a JSON reader for assertions. */
final class TestSupport {

    static final String SECRET_KEY = "sk_abcdefghQijklmQabcdefghijklmn";

    private static final Gson GSON = new Gson();

    private TestSupport() {
    }

    /** Points the SDK's environment lookup at a fixed map, for the configuration tests. */
    static void withEnvironment(Map<String, String> values) {
        AbstractDregsClient.environment = values::get;
    }

    /** Puts the environment lookup back to the real one. */
    static void resetEnvironment() {
        AbstractDregsClient.environment = System::getenv;
    }

    /** Parses a request body the stub recorded, so assertions can read it as a map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> json(String body) {
        return GSON.fromJson(body, Map.class);
    }

    /** An exception body, as a map, for assertions about what the API said. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object body) {
        return (Map<String, Object>) body;
    }

    /** A backoff that never waits, so the retry tests run at full speed. */
    static RetryBackoff noWaiting() {
        return (attempt, retryAfterSeconds) -> 0;
    }

    /** A backoff that never waits but records what it was asked to wait for. */
    static final class RecordingBackoff implements RetryBackoff {

        private final List<Double> retryAfters = new ArrayList<>();

        @Override
        public long millis(int attempt, Double retryAfterSeconds) {
            retryAfters.add(retryAfterSeconds);

            return 0;
        }

        List<Double> retryAfters() {
            return List.copyOf(retryAfters);
        }
    }
}
