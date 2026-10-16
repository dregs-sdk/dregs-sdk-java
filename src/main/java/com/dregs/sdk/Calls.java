package com.dregs.sdk;

import com.dregs.sdk.model.Analysis;
import com.dregs.sdk.model.Device;
import com.dregs.sdk.model.Escalation;
import com.dregs.sdk.model.EscalationSummary;
import com.dregs.sdk.model.Event;
import com.dregs.sdk.model.Group;
import com.dregs.sdk.model.Identity;
import com.dregs.sdk.model.IdentityReview;
import com.dregs.sdk.model.LinkedIdentity;
import com.dregs.sdk.model.ScoreHistoryEntry;
import com.dregs.sdk.model.Scores;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Every endpoint the namespaces call, described once.
 *
 * <p>Each method names the endpoint and how to read its answer, and validates its arguments, so a
 * bad one is refused before any request is made. {@link Dregs} and {@link AsyncDregs} each run the
 * descriptions their own way; the namespaces on both are one line per method.
 */
final class Calls {

    private Calls() {
    }

    // -- identities ---------------------------------------------------------------------------

    static ListCall<Identity> identitiesList(IdentityListParams params) {
        return new ListCall<>("/identities", Identity::fromApi, orDefault(params));
    }

    static Call<Identity> identitiesGet(String identityId) {
        return Call.get(Paths.identity(identityId), object(Identity::fromApi));
    }

    static Call<Scores> identitiesScores(String identityId) {
        return Call.get(
                Paths.identity(identityId, "/scores"),
                payload -> Scores.fromApi(Json.asObjects(payload)));
    }

    static Call<Analysis> identitiesAnalysis(String identityId) {
        return Call.get(Paths.identity(identityId, "/analysis"), object(Analysis::fromApi));
    }

    static Call<Analysis> identitiesAnalysis(String identityId, long cycleId) {
        return Call.get(Paths.identity(identityId, "/analysis/" + cycleId), object(Analysis::fromApi));
    }

    static Call<List<ScoreHistoryEntry>> identitiesHistory(String identityId, Integer limit) {
        Map<String, Object> query = new LinkedHashMap<>();

        query.put("limit", limit);

        return new Call<>(
                "GET",
                Paths.identity(identityId, "/history"),
                query,
                null,
                list(ScoreHistoryEntry::fromApi));
    }

    static Call<List<LinkedIdentity>> identitiesLinks(String identityId) {
        return Call.get(Paths.identity(identityId, "/links"), list(LinkedIdentity::fromApi));
    }

    static Call<List<IdentityReview>> identitiesReviews(String identityId) {
        return Call.get(Paths.identity(identityId, "/reviews"), list(IdentityReview::fromApi));
    }

    static Call<IdentityReview> identitiesReview(String identityId, String feedback) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (feedback != null && !feedback.isEmpty()) {
            body.put("feedback", feedback);
        }

        return new Call<>(
                "POST",
                Paths.identity(identityId, "/actions/review"),
                null,
                body,
                object(IdentityReview::fromApi));
    }

    static Call<Void> identitiesAnalyze(String identityId) {
        return new Call<>("POST", Paths.identity(identityId, "/actions/analyze"), null, null, nothing());
    }

    static Call<Identity> identitiesUpdate(String identityId, IdentityUpdate update) {
        String path = Paths.identity(identityId);

        if (update == null) {
            throw new IllegalArgumentException("Pass disregarded, attributes, or both.");
        }

        return new Call<>("PATCH", path, null, update.body(), object(Identity::fromApi));
    }

    // -- groups -------------------------------------------------------------------------------

    static ListCall<Group> groupsList(GroupListParams params) {
        return new ListCall<>("/groups", Group::fromApi, orDefault(params));
    }

    static Call<Group> groupsGet(String type, String groupId) {
        return Call.get(Paths.group(type, groupId), object(Group::fromApi));
    }

    static Call<Group> groupsCreate(String type, String groupId, Map<String, Object> attributes) {
        Paths.segment(groupId, "A group id");

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("type", type == null || type.isEmpty() ? TrackGroup.DEFAULT_TYPE : type);
        body.put("id", groupId);
        body.put("attributes", TrackRequest.copyOf(attributes));

        return new Call<>("POST", "/groups", null, body, object(Group::fromApi));
    }

    static Call<Group> groupsUpdate(String type, String groupId, Map<String, Object> attributes) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("attributes", TrackRequest.copyOf(attributes));

        return new Call<>("PATCH", Paths.group(type, groupId), null, body, object(Group::fromApi));
    }

    static Call<Void> groupsMember(String method, String type, String groupId, String identityId) {
        String suffix = "/identities/" + Paths.segment(identityId, "An identity id");

        return new Call<>(method, Paths.group(type, groupId, suffix), null, null, nothing());
    }

    // -- events -------------------------------------------------------------------------------

    static ListCall<Event> eventsList(EventListParams params) {
        return new ListCall<>("/events", Event::fromApi, orDefault(params));
    }

    static Call<Event> eventsGet(String eventId) {
        return Call.get(Paths.event(eventId), object(Event::fromApi));
    }

    // -- devices ------------------------------------------------------------------------------

    static ListCall<Device> devicesList(DeviceListParams params) {
        return new ListCall<>("/devices", Device::fromApi, orDefault(params));
    }

    static Call<Device> devicesGet(String fingerprint) {
        return Call.get(Paths.device(fingerprint), object(Device::fromApi));
    }

    static Call<Device> devicesUpdate(String fingerprint, boolean disregarded) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("disregarded", disregarded);

        return new Call<>("PATCH", Paths.device(fingerprint), null, body, object(Device::fromApi));
    }

    // -- escalations --------------------------------------------------------------------------

    static ListCall<Escalation> escalationsList(EscalationListParams params) {
        return new ListCall<>("/escalations", Escalation::fromApi, orDefault(params));
    }

    static Call<Escalation> escalationsGet(long escalationId) {
        return Call.get(Paths.escalation(escalationId), object(Escalation::fromApi));
    }

    static Call<Escalation> escalationsUpdate(long escalationId, String status) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("status", EscalationListParams.status(status));

        return new Call<>(
                "PATCH", Paths.escalation(escalationId), null, body, object(Escalation::fromApi));
    }

    static Call<EscalationSummary> escalationsSummary() {
        return Call.get("/escalations/summary", object(EscalationSummary::fromApi));
    }

    // -- parsing ------------------------------------------------------------------------------

    private static <T> Function<Object, T> object(Function<Map<String, Object>, T> parse) {
        return payload -> parse.apply(Json.asObject(payload));
    }

    private static <T> Function<Object, List<T>> list(Function<Map<String, Object>, T> parse) {
        return payload -> Json.asObjects(payload).stream().map(parse).toList();
    }

    private static Function<Object, Void> nothing() {
        return payload -> null;
    }

    private static IdentityListParams orDefault(IdentityListParams params) {
        return params == null ? IdentityListParams.builder().build() : params;
    }

    private static GroupListParams orDefault(GroupListParams params) {
        return params == null ? GroupListParams.builder().build() : params;
    }

    private static EventListParams orDefault(EventListParams params) {
        return params == null ? EventListParams.builder().build() : params;
    }

    private static DeviceListParams orDefault(DeviceListParams params) {
        return params == null ? DeviceListParams.builder().build() : params;
    }

    private static EscalationListParams orDefault(EscalationListParams params) {
        return params == null ? EscalationListParams.builder().build() : params;
    }
}
