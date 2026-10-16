package com.dregs.sdk;

import static com.dregs.sdk.TestSupport.SECRET_KEY;
import static com.dregs.sdk.TestSupport.json;
import static com.dregs.sdk.TestSupport.query;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dregs.sdk.model.Analysis;
import com.dregs.sdk.model.Device;
import com.dregs.sdk.model.Escalation;
import com.dregs.sdk.model.EscalationSummary;
import com.dregs.sdk.model.Event;
import com.dregs.sdk.model.Group;
import com.dregs.sdk.model.Identity;
import com.dregs.sdk.model.IdentityReview;
import com.dregs.sdk.model.LinkedIdentity;
import com.dregs.sdk.model.Page;
import com.dregs.sdk.model.ScoreHistoryEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The groups, events, devices, and escalations namespaces, the rest of identities, and paging. */
class ResourcesTest {

    private static final String GROUP =
            """
            {
              "type": "organization",
              "id": "org_678",
              "displayName": "Acme Inc",
              "identityCount": 12,
              "createdAt": "2026-10-01T10:00:00Z",
              "lastTrackedAt": "2026-10-15T10:00:00Z",
              "data": {"name": "Acme Inc", "plan": "enterprise"}
            }
            """;

    private static Dregs clientFor(StubServer server) {
        return Dregs.builder().secretKey(SECRET_KEY).baseUrl(server.baseUrl()).maxRetries(0).build();
    }

    /** A {@code V1Page} body, as the API sends one. */
    static String page(String content, int number, boolean last, int total) {
        return "{\"pageNumber\":" + number
                + ",\"pageSize\":2"
                + ",\"totalPages\":" + (last ? number + 1 : number + 2)
                + ",\"totalElements\":" + total
                + ",\"content\":[" + content + "]"
                + ",\"first\":" + (number == 0)
                + ",\"last\":" + last + "}";
    }

    static String page(String content) {
        return page(content, 0, true, content.isEmpty() ? 0 : content.split("},\\{").length);
    }

    @Nested
    @DisplayName("paging")
    class Paging {

        @Test
        void sendsThePageAndFiltersAndDropsUnsetOnes() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("")));
                    Dregs client = clientFor(server)) {

                client.identities().list(IdentityListParams.builder()
                        .term("ada")
                        .page(2)
                        .pageSize(50)
                        .sort("humanityScore")
                        .order("asc")
                        .build());

                Map<String, String> params = query(server.lastRequest());

                assertThat(server.lastRequest().path()).isEqualTo("/api/identities");
                assertThat(params)
                        .containsEntry("term", "ada")
                        .containsEntry("pageNumber", "2")
                        .containsEntry("pageSize", "50")
                        .containsEntry("sort", "humanityScore")
                        .containsEntry("order", "ASC")
                        .doesNotContainKeys("group", "groupType", "device", "badge");
            }
        }

        @Test
        void aListWithNoParametersAsksForTheFirstPageAlone() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("")));
                    Dregs client = clientFor(server)) {

                client.events().list();

                assertThat(query(server.lastRequest())).containsOnlyKeys("pageNumber");
                assertThat(query(server.lastRequest())).containsEntry("pageNumber", "0");
            }
        }

        @Test
        void aPageIsIterableWithItsPosition() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, page("{\"id\":\"a\"},{\"id\":\"b\"}", 0, false, 5)));
                    Dregs client = clientFor(server)) {

                Page<Identity> result = client.identities().list();

                List<String> ids = new ArrayList<>();

                result.forEach(identity -> ids.add(identity.id()));

                assertThat(ids).containsExactly("a", "b");
                assertThat(result.size()).isEqualTo(2);
                assertThat(result.get(1).id()).isEqualTo("b");
                assertThat(result.totalElements()).isEqualTo(5);
                assertThat(result.totalPages()).isEqualTo(2);
                assertThat(result.first()).isTrue();
                assertThat(result.hasNext()).isTrue();
                assertThat(result.raw()).containsKey("content");
            }
        }

        @Test
        void listAllWalksEveryPage() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.json(200, page(
                                    "{\"type\":\"team\",\"id\":\"1\"},{\"type\":\"team\",\"id\":\"2\"}", 0, false, 3)))
                            .enqueue(StubServer.Reply.json(200, page("{\"type\":\"team\",\"id\":\"3\"}", 1, true, 3)));
                    Dregs client = clientFor(server)) {

                List<String> ids = new ArrayList<>();

                for (Group group : client.groups().listAll(GroupListParams.builder().type("team").build())) {
                    ids.add(group.id());
                }

                assertThat(ids).containsExactly("1", "2", "3");
                assertThat(server.requests())
                        .extracting(request -> query(request).get("pageNumber"))
                        .containsExactly("0", "1");
                assertThat(server.requests())
                        .allSatisfy(request -> assertThat(query(request)).containsEntry("type", "team"));
            }
        }

        @Test
        void listAllFetchesNothingUntilTheLoopStarts() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("")));
                    Dregs client = clientFor(server)) {

                Iterable<Device> devices = client.devices().listAll();

                assertThat(server.callCount()).isZero();

                Iterator<Device> iterator = devices.iterator();

                assertThat(iterator.hasNext()).isFalse();
                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void listAllStopsOnAnEmptyPageEvenIfNotMarkedLast() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("", 0, false, 0)));
                    Dregs client = clientFor(server)) {

                assertThat(client.events().listAll()).isEmpty();
                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void refusesANegativePage() {
            assertThatThrownBy(() -> DeviceListParams.builder().page(-1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("from 0");
        }

        @Test
        void refusesAnUnknownOrder() {
            assertThatThrownBy(() -> EscalationListParams.builder().order("sideways").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("asc");
        }

        @Test
        void acceptsEitherCaseForTheOrder() {
            assertThat(EventListParams.builder().order("Desc").build().order()).isEqualTo("DESC");
        }
    }

    @Nested
    @DisplayName("identities")
    class IdentitiesCalls {

        @Test
        void analysisOfAPastCycle() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"id\":2000871,\"identityId\":\"user_12345\",\"scores\":[]}"));
                    Dregs client = clientFor(server)) {

                Analysis analysis = client.identities().analysis("user_12345", 2000871L);

                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/user_12345/analysis/2000871");
                assertThat(analysis.id()).isEqualTo(2000871L);
            }
        }

        @Test
        void history() {
            String body = "[{\"id\":9,\"humanityScore\":80,\"behaviorScore\":60,\"eventCount\":3,"
                    + "\"finishedAt\":\"2026-10-01T00:00:00Z\"}]";

            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, body));
                    Dregs client = clientFor(server)) {

                List<ScoreHistoryEntry> history = client.identities().history("user_12345", 10);

                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/user_12345/history");
                assertThat(query(server.lastRequest())).containsEntry("limit", "10");
                assertThat(history.get(0).cycleId()).isEqualTo(9L);
                assertThat(history.get(0).humanity()).isEqualTo(80);
                assertThat(history.get(0).authenticity()).isNull();
                assertThat(history.get(0).finishedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
            }
        }

        @Test
        void historyWithoutALimitSendsNone() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "[]"));
                    Dregs client = clientFor(server)) {

                assertThat(client.identities().history("user_12345")).isEmpty();
                assertThat(server.lastRequest().query()).isNull();
            }
        }

        @Test
        void links() {
            String body = "[{\"id\":1,\"targetIdentityId\":\"user_999\",\"targetDisplayName\":\"Bob\","
                    + "\"type\":\"SAME_DEVICE\",\"value\":0.9,\"confidence\":0.8}]";

            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, body));
                    Dregs client = clientFor(server)) {

                List<LinkedIdentity> links = client.identities().links("user_12345");

                assertThat(links.get(0).identityId()).isEqualTo("user_999");
                assertThat(links.get(0).displayName()).isEqualTo("Bob");
                assertThat(links.get(0).type()).isEqualTo("SAME_DEVICE");
                assertThat(links.get(0).value()).isEqualTo(0.9);
            }
        }

        @Test
        void updateSendsOnlyWhatWasGiven() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, "{\"id\":\"user_12345\",\"disregarded\":true}"));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities()
                        .update("user_12345", IdentityUpdate.builder().disregarded(true).build());

                assertThat(server.lastRequest().method()).isEqualTo("PATCH");
                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/user_12345");
                assertThat(json(server.lastRequest().body())).isEqualTo(Map.of("disregarded", true));
                assertThat(identity.disregarded()).isTrue();
            }
        }

        @Test
        void updateCanMergeAttributes() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "{\"id\":\"user_12345\"}"));
                    Dregs client = clientFor(server)) {

                client.identities().update(
                        "user_12345", IdentityUpdate.builder().attributes(Map.of("plan", "pro")).build());

                assertThat(json(server.lastRequest().body()))
                        .isEqualTo(Map.of("attributes", Map.of("plan", "pro")));
            }
        }

        @Test
        void updateWithNothingIsRefused() {
            assertThatThrownBy(() -> IdentityUpdate.builder().build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("disregarded");
        }

        @Test
        void reviewAndReviews() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.json(201, "{\"id\":7,\"identityId\":\"user_12345\","
                                    + "\"status\":\"PENDING\",\"userFeedback\":\"real customer\"}"))
                            .enqueue(StubServer.Reply.json(
                                    200, "[{\"id\":7,\"status\":\"FINISHED\",\"summary\":\"Looks fine.\"}]"));
                    Dregs client = clientFor(server)) {

                IdentityReview review = client.identities().review("user_12345", "real customer");

                StubServer.Recorded post = server.lastRequest();

                List<IdentityReview> reviews = client.identities().reviews("user_12345");

                assertThat(post.method()).isEqualTo("POST");
                assertThat(post.path()).isEqualTo("/api/identities/user_12345/actions/review");
                assertThat(json(post.body())).isEqualTo(Map.of("feedback", "real customer"));
                assertThat(review.status()).isEqualTo("PENDING");
                assertThat(review.feedback()).isEqualTo("real customer");
                assertThat(review.id()).isEqualTo(7L);
                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/user_12345/reviews");
                assertThat(reviews.get(0).summary()).isEqualTo("Looks fine.");
            }
        }

        @Test
        void aReviewWithoutFeedbackSendsAnEmptyObject() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(201, "{\"id\":7}"));
                    Dregs client = clientFor(server)) {

                client.identities().review("user_12345");

                assertThat(json(server.lastRequest().body())).isEmpty();
            }
        }

        @Test
        void listFilters() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("")));
                    Dregs client = clientFor(server)) {

                client.identities().list(IdentityListParams.builder()
                        .group("org_678")
                        .groupType("organization")
                        .device("abc123")
                        .badge("behavior.account-takeover-signal")
                        .build());

                assertThat(query(server.lastRequest()))
                        .containsEntry("group", "org_678")
                        .containsEntry("groupType", "organization")
                        .containsEntry("device", "abc123")
                        .containsEntry("badge", "behavior.account-takeover-signal");
            }
        }
    }

    @Nested
    @DisplayName("groups")
    class GroupsCalls {

        @Test
        void get() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, GROUP));
                    Dregs client = clientFor(server)) {

                Group group = client.groups().get("organization", "org_678");

                assertThat(server.lastRequest().path()).isEqualTo("/api/groups/organization/org_678");
                assertThat(group.displayName()).isEqualTo("Acme Inc");
                assertThat(group.identityCount()).isEqualTo(12);
                assertThat(group.data()).containsEntry("plan", "enterprise");
                assertThat(group.createdAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
            }
        }

        @Test
        void createDefaultsTheTypeAndSendsAttributes() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(201, GROUP));
                    Dregs client = clientFor(server)) {

                client.groups().create("org_678", Map.of("name", "Acme Inc"));

                assertThat(server.lastRequest().method()).isEqualTo("POST");
                assertThat(server.lastRequest().path()).isEqualTo("/api/groups");
                assertThat(json(server.lastRequest().body()))
                        .isEqualTo(Map.of(
                                "type", "organization", "id", "org_678", "attributes", Map.of("name", "Acme Inc")));
            }
        }

        @Test
        void createWithNoAttributesSendsAnEmptyObject() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(201, GROUP));
                    Dregs client = clientFor(server)) {

                client.groups().create("team", "team_42", null);

                Map<String, Object> body = json(server.lastRequest().body());

                assertThat(body).containsEntry("type", "team").containsEntry("attributes", Map.of());
            }
        }

        @Test
        void update() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, GROUP));
                    Dregs client = clientFor(server)) {

                client.groups().update("team", "team_42", Map.of("name", "Payments"));

                assertThat(server.lastRequest().method()).isEqualTo("PATCH");
                assertThat(server.lastRequest().path()).isEqualTo("/api/groups/team/team_42");
                assertThat(json(server.lastRequest().body()))
                        .isEqualTo(Map.of("attributes", Map.of("name", "Payments")));
            }
        }

        @Test
        void membership() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(204));
                    Dregs client = clientFor(server)) {

                client.groups().addMember("team", "team_42", "ada@example.com");
                client.groups().removeMember("team", "team_42", "ada@example.com");

                assertThat(server.requests())
                        .extracting(request -> request.method() + " " + request.path())
                        .containsExactly(
                                "PUT /api/groups/team/team_42/identities/ada%40example.com",
                                "DELETE /api/groups/team/team_42/identities/ada%40example.com");
            }
        }

        @Test
        void aMissingIdIsRefusedBeforeAnyRequest() {
            try (StubServer server = new StubServer();
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.groups().get("team", ""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("group id");
                assertThatThrownBy(() -> client.groups().create(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("group id");
                assertThatThrownBy(() -> client.groups().addMember("team", "team_42", null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("identity id");
                assertThat(server.callCount()).isZero();
            }
        }
    }

    @Nested
    @DisplayName("events")
    class EventsCalls {

        @Test
        void getParsesGroupsAndIdentity() {
            String body =
                    """
                    {
                      "id": "signup-991",
                      "timestamp": "2026-10-15T10:00:00Z",
                      "identity": {"id": "user_12345", "label": "Ada", "path": "/api/identities/user_12345"},
                      "groups": [
                        {"type": "team", "id": "team_42", "label": "Payments", "path": "/api/groups/team/team_42"}
                      ],
                      "eventSource": "java-sdk",
                      "eventName": "user.signup",
                      "canonicalType": "SIGNUP",
                      "eventData": {"plan": "pro"}
                    }
                    """;

            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, body));
                    Dregs client = clientFor(server)) {

                Event event = client.events().get("signup-991");

                assertThat(server.lastRequest().path()).isEqualTo("/api/events/signup-991");
                assertThat(event.name()).isEqualTo("user.signup");
                assertThat(event.source()).isEqualTo("java-sdk");
                assertThat(event.canonicalType()).isEqualTo("SIGNUP");
                assertThat(event.identity().id()).isEqualTo("user_12345");
                assertThat(event.identity().label()).isEqualTo("Ada");
                assertThat(event.groups().get(0).type()).isEqualTo("team");
                assertThat(event.groups().get(0).label()).isEqualTo("Payments");
                assertThat(event.data()).containsEntry("plan", "pro");
                assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-10-15T10:00:00Z"));
            }
        }

        @Test
        void anAnonymousEventHasNoIdentity() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "{\"id\":\"e1\"}"));
                    Dregs client = clientFor(server)) {

                Event event = client.events().get("e1");

                assertThat(event.identity()).isNull();
                assertThat(event.groups()).isEmpty();
                assertThat(event.data()).isEmpty();
            }
        }

        @Test
        void listFilters() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("")));
                    Dregs client = clientFor(server)) {

                client.events().list(EventListParams.builder()
                        .identity("user_12345")
                        .group("team_42")
                        .groupType("team")
                        .session("s1")
                        .build());

                assertThat(query(server.lastRequest()))
                        .containsEntry("identity", "user_12345")
                        .containsEntry("group", "team_42")
                        .containsEntry("groupType", "team")
                        .containsEntry("session", "s1");
            }
        }
    }

    @Nested
    @DisplayName("devices")
    class DevicesCalls {

        @Test
        void update() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"fingerprint\":\"abc123\",\"disregarded\":true,\"countryCode\":\"US\","
                                            + "\"latitude\":40.7}"));
                    Dregs client = clientFor(server)) {

                Device device = client.devices().update("abc123", true);

                assertThat(server.lastRequest().method()).isEqualTo("PATCH");
                assertThat(server.lastRequest().path()).isEqualTo("/api/devices/abc123");
                assertThat(json(server.lastRequest().body())).isEqualTo(Map.of("disregarded", true));
                assertThat(device.disregarded()).isTrue();
                assertThat(device.countryCode()).isEqualTo("US");
                assertThat(device.latitude()).isEqualTo(40.7);
            }
        }
    }

    @Nested
    @DisplayName("escalations")
    class EscalationsCalls {

        @Test
        void listUpperCasesStatusAndSeverity() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, page("")));
                    Dregs client = clientFor(server)) {

                client.escalations().list(EscalationListParams.builder()
                        .status("open")
                        .severity("critical")
                        .rule(3)
                        .build());

                assertThat(query(server.lastRequest()))
                        .containsEntry("status", "OPEN")
                        .containsEntry("severity", "CRITICAL")
                        .containsEntry("rule", "3");
            }
        }

        @Test
        void update() {
            String body =
                    """
                    {
                      "id": 41,
                      "status": "ACKNOWLEDGED",
                      "severity": "WARNING",
                      "identity": {"id": "user_12345"},
                      "escalationRule": {"id": "3", "label": "Low humanity"},
                      "scoreSnapshot": {"humanity": 12}
                    }
                    """;

            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, body));
                    Dregs client = clientFor(server)) {

                Escalation escalation = client.escalations().update(41, "acknowledged");

                assertThat(server.lastRequest().method()).isEqualTo("PATCH");
                assertThat(server.lastRequest().path()).isEqualTo("/api/escalations/41");
                assertThat(json(server.lastRequest().body())).isEqualTo(Map.of("status", "ACKNOWLEDGED"));
                assertThat(escalation.id()).isEqualTo(41L);
                assertThat(escalation.identity().id()).isEqualTo("user_12345");
                assertThat(escalation.rule().label()).isEqualTo("Low humanity");
                assertThat(escalation.scores()).containsEntry("humanity", 12L);
            }
        }

        @Test
        void updateRefusesAnUnknownStatus() {
            try (StubServer server = new StubServer();
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.escalations().update(41, "snoozed"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("acknowledged");
                assertThat(server.callCount()).isZero();
            }
        }

        @Test
        void listRefusesAnUnknownStatus() {
            assertThatThrownBy(() -> EscalationListParams.builder().status("snoozed").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        void summary() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, "{\"open\":3,\"acknowledged\":1,\"closed\":20}"));
                    Dregs client = clientFor(server)) {

                EscalationSummary summary = client.escalations().summary();

                assertThat(server.lastRequest().path()).isEqualTo("/api/escalations/summary");
                assertThat(summary.open()).isEqualTo(3);
                assertThat(summary.acknowledged()).isEqualTo(1);
                assertThat(summary.closed()).isEqualTo(20);
            }
        }
    }

    @Nested
    @DisplayName("async")
    class Async {

        private AsyncDregs asyncClientFor(StubServer server) {
            return AsyncDregs.builder().secretKey(SECRET_KEY).baseUrl(server.baseUrl()).maxRetries(0).build();
        }

        @Test
        void listAllWalksEveryPage() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.json(200, page("{\"fingerprint\":\"a\"}", 0, false, 2)))
                            .enqueue(StubServer.Reply.json(200, page("{\"fingerprint\":\"b\"}", 1, true, 2)));
                    AsyncDregs client = asyncClientFor(server)) {

                List<Device> devices = client.devices().listAll().join();

                assertThat(devices.stream().map(Device::fingerprint).collect(Collectors.toList()))
                        .containsExactly("a", "b");
                assertThat(server.callCount()).isEqualTo(2);
            }
        }

        @Test
        void groupsCreate() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(201, GROUP));
                    AsyncDregs client = asyncClientFor(server)) {

                Group group = client.groups().create("org_678", Map.of("name", "Acme Inc")).join();

                assertThat(server.lastRequest().path()).isEqualTo("/api/groups");
                assertThat(group.id()).isEqualTo("org_678");
            }
        }

        @Test
        void anInvalidArgumentIsThrownBeforeAnyFuture() {
            try (StubServer server = new StubServer();
                    AsyncDregs client = asyncClientFor(server)) {

                assertThatThrownBy(() -> client.escalations().update(41, "snoozed"))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> client.events().get(""))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThat(server.callCount()).isZero();
            }
        }
    }
}
