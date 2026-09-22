package com.dregs.model;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reading fields out of a parsed JSON payload, leniently.
 *
 * <p>Nothing here throws on a field that is missing, null, or the wrong shape: it answers null or
 * an empty collection instead. An SDK that refuses to parse a response it half-understands ages
 * badly, and the raw payload is kept on every model for whatever this could not make sense of.
 */
final class ApiData {

    private ApiData() {
    }

    static String string(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        return value instanceof String text ? text : null;
    }

    static Integer integer(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        return value instanceof Number number ? number.intValue() : null;
    }

    static Long longValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        return value instanceof Number number ? number.longValue() : null;
    }

    static Double decimal(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        return value instanceof Number number ? number.doubleValue() : null;
    }

    static boolean bool(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        return value instanceof Boolean flag && flag;
    }

    /** Parses an ISO-8601 timestamp, answering null for anything unparseable. */
    static Instant instant(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (!(value instanceof String text) || text.isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // The API sends UTC with a Z, but an offset or a local time should not cost the
            // caller the whole response.
            return offsetOrLocal(text);
        }
    }

    private static Instant offsetOrLocal(String text) {
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return java.time.LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException stillUnreadable) {
                return null;
            }
        }
    }

    /** The nested object at {@code key}, or an empty map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        return value instanceof Map ? immutableMap((Map<String, Object>) value) : Map.of();
    }

    /** The objects in the array at {@code key}, skipping any element that is not one. */
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> objects(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (!(value instanceof List<?> items)) {
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

    /**
     * An unmodifiable copy that tolerates null values.
     *
     * <p>{@code Map.copyOf} would throw on the JSON nulls the API sends for fields an identity has
     * not filled in yet, which is exactly the data this has to survive.
     */
    static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /** An unmodifiable copy of a list that tolerates null elements. */
    static <T> List<T> immutableList(List<T> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(new ArrayList<>(source));
    }
}
