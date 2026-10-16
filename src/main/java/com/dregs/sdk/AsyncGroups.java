package com.dregs.sdk;

import com.dregs.sdk.model.Group;
import com.dregs.sdk.model.Page;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The async twin of {@link Groups}.
 *
 * <p>Reach one through {@link AsyncDregs#groups()}. Each method does what the synchronous one
 * documents; the failures it would throw arrive as the future's cause instead.
 */
public final class AsyncGroups {

    private final AsyncDregs client;

    AsyncGroups(AsyncDregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of groups. See {@link Groups#list()}.
     *
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Group>> list() {
        return list(null);
    }

    /**
     * Returns one page of groups. See {@link Groups#list(GroupListParams)}.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return a future carrying the page
     */
    public CompletableFuture<Page<Group>> list(GroupListParams params) {
        ListCall<Group> call = Calls.groupsList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every group. See {@link #listAll(GroupListParams)}.
     *
     * @return a future carrying the groups
     */
    public CompletableFuture<List<Group>> listAll() {
        return listAll(null);
    }

    /**
     * Every group that matches, from every page.
     *
     * <p>The pages are fetched one after another and the future completes with all of them.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return a future carrying the groups
     */
    public CompletableFuture<List<Group>> listAll(GroupListParams params) {
        return client.walk(Calls.groupsList(params));
    }

    /**
     * Returns the group, with its attributes.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @return a future carrying the group
     */
    public CompletableFuture<Group> get(String type, String groupId) {
        return client.run(Calls.groupsGet(type, groupId));
    }

    /**
     * Creates an organization with no attributes. See {@link Groups#create(String)}.
     *
     * @param groupId your own id for the group
     * @return a future carrying the group
     */
    public CompletableFuture<Group> create(String groupId) {
        return create(null, groupId, null);
    }

    /**
     * Creates an organization, or merges into it. See {@link Groups#create(String, Map)}.
     *
     * @param groupId your own id for the group
     * @param attributes attributes such as a name or plan
     * @return a future carrying the group
     */
    public CompletableFuture<Group> create(String groupId, Map<String, Object> attributes) {
        return create(null, groupId, attributes);
    }

    /**
     * Creates a group, or merges into it. See {@link Groups#create(String, String, Map)}.
     *
     * @param type the kind of group, defaulting to {@code "organization"} when {@code null}
     * @param groupId your own id for the group
     * @param attributes attributes such as a name or plan, or {@code null} for none
     * @return a future carrying the group
     */
    public CompletableFuture<Group> create(String type, String groupId, Map<String, Object> attributes) {
        return client.run(Calls.groupsCreate(type, groupId, attributes));
    }

    /**
     * Merges attributes into the group and returns it.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @param attributes the attributes to merge
     * @return a future carrying the group, as it is now
     */
    public CompletableFuture<Group> update(String type, String groupId, Map<String, Object> attributes) {
        return client.run(Calls.groupsUpdate(type, groupId, attributes));
    }

    /**
     * Makes the identity a member of the group.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @param identityId your own id for the user
     * @return a future that completes once the identity is a member
     */
    public CompletableFuture<Void> addMember(String type, String groupId, String identityId) {
        return client.run(Calls.groupsMember("PUT", type, groupId, identityId));
    }

    /**
     * Removes the identity from the group.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @param identityId your own id for the user
     * @return a future that completes once the identity is no longer a member
     */
    public CompletableFuture<Void> removeMember(String type, String groupId, String identityId) {
        return client.run(Calls.groupsMember("DELETE", type, groupId, identityId));
    }
}
