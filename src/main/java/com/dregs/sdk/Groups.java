package com.dregs.sdk;

import com.dregs.sdk.model.Group;
import com.dregs.sdk.model.Page;
import java.util.Map;

/**
 * Reading and managing groups: the organizations, teams, or workspaces your users belong to.
 *
 * <p>Reach one through {@link Dregs#groups()}. {@link Dregs#track(TrackRequest)} already records
 * groups and memberships as events arrive. These calls are for reading them back, and for keeping
 * Dregs in step when something changes outside an event, such as a user leaving a team.
 *
 * <p>A group is identified by its type and id together, and every call that names an existing
 * group takes them in that order, as {@link TrackRequest.Builder#group(String, String, Map)} does.
 */
public final class Groups {

    private final Dregs client;

    Groups(Dregs client) {
        this.client = client;
    }

    /**
     * Returns the first page of groups, most recently active first.
     *
     * @return the page
     */
    public Page<Group> list() {
        return list(null);
    }

    /**
     * Returns one page of groups.
     *
     * <p>The groups in a list leave out their attributes; {@link #get(String, String)} returns them.
     * Use {@link #listAll(GroupListParams)} to walk every page.
     *
     * @param params the page, its size and order, and the filters; {@code null} for the defaults
     * @return the page
     */
    public Page<Group> list(GroupListParams params) {
        ListCall<Group> call = Calls.groupsList(params);

        return client.run(call.page(call.params().page()));
    }

    /**
     * Every group, fetching pages as the iteration reaches them.
     *
     * @return the groups
     */
    public Iterable<Group> listAll() {
        return listAll(null);
    }

    /**
     * Every group that matches, fetching pages as the iteration reaches them.
     *
     * <p>Nothing is fetched until the loop starts, and each new loop starts a fresh walk.
     *
     * @param params the page size, order, and filters; {@code null} for the defaults
     * @return the groups
     */
    public Iterable<Group> listAll(GroupListParams params) {
        return client.walk(Calls.groupsList(params));
    }

    /**
     * Returns the group, with its attributes.
     *
     * @param type the kind of group, such as {@code "organization"} or {@code "team"}
     * @param groupId your own id for the group
     * @return the group
     * @throws com.dregs.sdk.exception.NotFoundException when no group of this type has this id
     * @throws IllegalArgumentException when the type or id is empty
     */
    public Group get(String type, String groupId) {
        return client.run(Calls.groupsGet(type, groupId));
    }

    /**
     * Creates an organization with no attributes, or does nothing when it already exists.
     *
     * @param groupId your own id for the group
     * @return the group
     * @throws IllegalArgumentException when the id is empty
     */
    public Group create(String groupId) {
        return create(null, groupId, null);
    }

    /**
     * Creates an organization, or merges the attributes into the one that already has this id.
     *
     * @param groupId your own id for the group
     * @param attributes attributes such as a name or plan
     * @return the group
     * @throws IllegalArgumentException when the id is empty
     */
    public Group create(String groupId, Map<String, Object> attributes) {
        return create(null, groupId, attributes);
    }

    /**
     * Creates a group, or merges the attributes into the group that already has this type and id.
     *
     * <p>Because it merges, this is safe to call on every sync.
     *
     * @param type the kind of group, defaulting to {@code "organization"} when {@code null}
     * @param groupId your own id for the group
     * @param attributes attributes such as a name or plan, or {@code null} for none
     * @return the group
     * @throws IllegalArgumentException when the id is empty
     */
    public Group create(String type, String groupId, Map<String, Object> attributes) {
        return client.run(Calls.groupsCreate(type, groupId, attributes));
    }

    /**
     * Merges attributes into the group and returns it.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @param attributes the attributes to merge
     * @return the group, as it is now
     * @throws com.dregs.sdk.exception.NotFoundException when no group of this type has this id
     * @throws IllegalArgumentException when the type or id is empty
     */
    public Group update(String type, String groupId, Map<String, Object> attributes) {
        return client.run(Calls.groupsUpdate(type, groupId, attributes));
    }

    /**
     * Makes the identity a member of the group. Adding an existing member does nothing.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @param identityId your own id for the user
     * @throws com.dregs.sdk.exception.NotFoundException when the group or the identity is unknown
     * @throws IllegalArgumentException when any of the three is empty
     */
    public void addMember(String type, String groupId, String identityId) {
        client.run(Calls.groupsMember("PUT", type, groupId, identityId));
    }

    /**
     * Removes the identity from the group, such as when a user leaves a team.
     *
     * @param type the kind of group
     * @param groupId your own id for the group
     * @param identityId your own id for the user
     * @throws com.dregs.sdk.exception.NotFoundException when the group or the identity is unknown
     * @throws IllegalArgumentException when any of the three is empty
     */
    public void removeMember(String type, String groupId, String identityId) {
        client.run(Calls.groupsMember("DELETE", type, groupId, identityId));
    }
}
