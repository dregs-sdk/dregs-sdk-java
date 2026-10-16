package com.dregs.sdk;

import com.dregs.sdk.model.Page;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A list endpoint and its filters, from which any one page's {@link Call} can be made.
 *
 * @param path the list endpoint
 * @param parse builds one item from its parsed object
 * @param params the caller's paging and filters
 * @param <T> the type of the items
 */
record ListCall<T>(String path, Function<Map<String, Object>, T> parse, ListParams params) {

    /** The call for one page. */
    Call<Page<T>> page(int number) {
        Map<String, Object> query = new LinkedHashMap<>(params.filters());

        query.put("pageNumber", number);
        query.put("pageSize", params.pageSize());
        query.put("sort", params.sort());
        query.put("order", params.order());

        return new Call<>(
                "GET", path, query, null, payload -> Page.fromApi(Json.asObject(payload), parse));
    }
}
