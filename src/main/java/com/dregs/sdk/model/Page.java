package com.dregs.sdk.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One page of a list call.
 *
 * <p>Iterable over the items on this page, and carrying the page's position and the totals across
 * every page. Pages are numbered from 0. To walk every page, use the namespace's {@code listAll}
 * rather than looping on {@link #pageNumber()} yourself:
 *
 * <pre>{@code
 * Page<Identity> page = client.identities().list(IdentityListParams.builder().pageSize(50).build());
 *
 * page.totalElements();  // how many match in all
 * page.hasNext();        // whether another page follows
 * }</pre>
 *
 * @param <T> the type of the items
 * @param items the items on this page
 * @param pageNumber this page's number, counting from 0
 * @param pageSize the most items a page holds
 * @param totalPages how many pages there are in all
 * @param totalElements how many items there are across every page
 * @param first whether this is the first page
 * @param last whether this is the last page
 * @param raw the payload this was built from
 */
public record Page<T>(
        List<T> items,
        int pageNumber,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean first,
        boolean last,
        Map<String, Object> raw) implements Iterable<T> {

    /**
     * Canonical constructor, which takes defensive copies of the collections.
     */
    public Page {
        items = ApiData.immutableList(items);
        raw = ApiData.immutableMap(raw);
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    /**
     * How many items are on this page.
     *
     * @return the number of items on this page, not across every page
     */
    public int size() {
        return items.size();
    }

    /**
     * Whether this page has no items.
     *
     * @return true when the page is empty
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Returns one item on this page.
     *
     * @param index the item's position on this page, counting from 0
     * @return the item
     * @throws IndexOutOfBoundsException when the page has no item at that position
     */
    public T get(int index) {
        return items.get(index);
    }

    /**
     * Whether another page follows this one.
     *
     * <p>An empty page never has a next one, even when the API has not marked it last, so a loop on
     * this cannot run forever.
     *
     * @return true when there is a next page to fetch
     */
    public boolean hasNext() {
        return !last && !items.isEmpty();
    }

    /**
     * Builds a page from a parsed list response.
     *
     * @param <T> the type of the items
     * @param payload the parsed page object, with its items under {@code content}
     * @param parse builds one item from its parsed object
     * @return the page
     */
    public static <T> Page<T> fromApi(
            Map<String, Object> payload, Function<Map<String, Object>, T> parse) {
        Map<String, Object> body = payload == null ? Map.of() : payload;
        Long totalElements = ApiData.longValue(body, "totalElements");

        return new Page<>(
                ApiData.objects(body, "content").stream().map(parse).toList(),
                orZero(ApiData.integer(body, "pageNumber")),
                orZero(ApiData.integer(body, "pageSize")),
                orZero(ApiData.integer(body, "totalPages")),
                totalElements == null ? 0 : totalElements,
                ApiData.bool(body, "first", true),
                ApiData.bool(body, "last", true),
                body);
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
