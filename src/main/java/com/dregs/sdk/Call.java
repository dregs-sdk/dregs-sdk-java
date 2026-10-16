package com.dregs.sdk;

import java.util.Map;
import java.util.function.Function;

/**
 * One API call, described once so the sync and async namespaces both run the same description and
 * cannot drift.
 *
 * @param method the HTTP method
 * @param path the path below the base URL, already escaped
 * @param query query parameters, of which unset ones are left out; may be null
 * @param body the JSON body, or null for none
 * @param parse turns the parsed response into what the caller gets back
 * @param <T> what the call returns
 */
record Call<T>(
        String method,
        String path,
        Map<String, Object> query,
        Map<String, Object> body,
        Function<Object, T> parse) {

    static <T> Call<T> get(String path, Function<Object, T> parse) {
        return new Call<>("GET", path, null, null, parse);
    }
}
