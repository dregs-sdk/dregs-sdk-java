package com.dregs.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The SDK's only contact with a JSON library.
 *
 * <p>Gson is confined to this class on purpose. Nothing it defines appears in a public signature:
 * requests go out as maps and responses come back as {@code Map}, {@code List}, {@code String},
 * {@code Number}, {@code Boolean}, and null. That keeps the dependency an implementation detail
 * rather than something a caller has to agree with, which matters in an application that has
 * already picked a different JSON library.
 */
final class Json {

    /**
     * Whole numbers parse as {@code Long} and decimals as {@code Double}. Gson's default would
     * make every number a {@code Double}, which turns an event count of 47 into "47.0" the first
     * time someone prints one.
     */
    private static final Gson GSON =
            new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    private Json() {
    }

    /** Serializes a request body. */
    static String write(Object value) {
        return GSON.toJson(value);
    }

    /**
     * Parses a response body, answering null for anything unparseable.
     *
     * <p>An error response is often not JSON at all: a proxy's HTML page, or nothing. The status
     * code is what the SDK acts on, so a body it cannot read costs nothing.
     */
    static Object parseOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return GSON.fromJson(text, Object.class);
        } catch (JsonSyntaxException ignored) {
            return null;
        }
    }

    /**
     * Parses a body that has to be JSON, throwing when it is not.
     *
     * @throws JsonSyntaxException when the text is not JSON
     */
    static Object parseStrict(String text) {
        return GSON.fromJson(text, Object.class);
    }

    /** A parsed payload as a JSON object, or an empty map when it was anything else. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object parsed) {
        return parsed instanceof Map ? (Map<String, Object>) parsed : Map.of();
    }

    /** A parsed payload as a list of JSON objects, skipping any element that is not one. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> asObjects(Object parsed) {
        if (!(parsed instanceof List<?> items)) {
            return List.of();
        }

        List<Map<String, Object>> objects = new ArrayList<>(items.size());

        for (Object item : items) {
            if (item instanceof Map) {
                objects.add((Map<String, Object>) item);
            }
        }

        return objects;
    }
}
