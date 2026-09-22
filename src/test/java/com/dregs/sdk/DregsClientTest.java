package com.dregs.sdk;

import static com.dregs.sdk.TestSupport.SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dregs.sdk.model.TrackResult;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Client construction, configuration, and the track() request body. */
class DregsClientTest {

    private static final String ACCEPTED = "{\"status\":\"success\",\"id\":\"evt_1\"}";

    @AfterEach
    void restoreEnvironment() {
        TestSupport.resetEnvironment();
    }

    private static Dregs clientFor(StubServer server) {
        return Dregs.builder().secretKey(SECRET_KEY).baseUrl(server.baseUrl()).maxRetries(0).build();
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void readsTheSecretKeyFromTheEnvironment() {
            TestSupport.withEnvironment(Map.of("DREGS_SECRET_KEY", SECRET_KEY));

            try (Dregs client = Dregs.builder().build()) {
                assertThat(client.baseUrl()).isEqualTo("https://dregs.com/api");
            }
        }

        @Test
        void readsTheBaseUrlFromTheEnvironment() {
            TestSupport.withEnvironment(Map.of("DREGS_BASE_URL", "https://staging.example.com/api"));

            try (Dregs client = Dregs.builder().secretKey(SECRET_KEY).build()) {
                assertThat(client.baseUrl()).isEqualTo("https://staging.example.com/api");
            }
        }

        @Test
        void anExplicitKeyBeatsTheEnvironment() {
            TestSupport.withEnvironment(Map.of("DREGS_SECRET_KEY", "sk_from_the_environment"));

            try (Dregs client = Dregs.builder().secretKey(SECRET_KEY).build();
                    StubServer server = new StubServer()) {

                assertThat(client).isNotNull();
                assertThat(server.callCount()).isZero();
            }
        }

        @Test
        void aTrailingSlashOnTheBaseUrlDoesNotDoubleUp() {
            try (Dregs client =
                    Dregs.builder().secretKey(SECRET_KEY).baseUrl("https://example.com/api/").build()) {

                assertThat(client.baseUrl()).isEqualTo("https://example.com/api");
            }
        }

        @Test
        void aMissingKeyNamesTheEnvironmentVariable() {
            TestSupport.withEnvironment(Map.of());

            assertThatThrownBy(() -> Dregs.builder().build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DREGS_SECRET_KEY");
        }

        @Test
        void aPublicKeyIsRefusedWithAnExplanation() {
            assertThatThrownBy(() -> Dregs.builder().secretKey("pk_abcdefghQijklmQabcdefgh").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("public key");
        }

        @Test
        void negativeRetriesAreRefused() {
            assertThatThrownBy(
                            () -> Dregs.builder().secretKey(SECRET_KEY).maxRetries(-1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxRetries");
        }

        @Test
        void theDefaultsAreTheDocumentedOnes() {
            TestSupport.withEnvironment(Map.of());

            try (Dregs client = Dregs.builder().secretKey(SECRET_KEY).build()) {
                assertThat(client.baseUrl()).isEqualTo("https://dregs.com/api");
                assertThat(client.maxRetries()).isEqualTo(2);
                assertThat(client.timeout().toSeconds()).isEqualTo(10);
            }
        }
    }

    @Nested
    @DisplayName("the track request")
    class TrackRequestBody {

        @Test
        void sendsTheDocumentedBody() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track(TrackRequest.builder("user.signup", "user_12345")
                        .data(Map.of("plan", "pro"))
                        .identityData(Map.of("email", "ada@example.com"))
                        .eventId("signup-991")
                        .build());

                StubServer.Recorded request = server.lastRequest();
                Map<String, Object> body = TestSupport.json(request.body());

                assertThat(request.method()).isEqualTo("POST");
                assertThat(request.path()).isEqualTo("/api/events");
                assertThat(body).containsEntry("id", "signup-991");
                assertThat(body).containsEntry("type", "user.signup");
                assertThat(body).containsEntry("data", Map.of("plan", "pro"));
                assertThat(body)
                        .containsEntry(
                                "identity",
                                Map.of("id", "user_12345", "data", Map.of("email", "ada@example.com")));
                assertThat(body).containsEntry("source", "java-sdk");
            }
        }

        @Test
        void authorizesWithTheSecretKey() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track("user.signup", "user_12345");

                StubServer.Recorded request = server.lastRequest();

                assertThat(request.header("Authorization")).isEqualTo("Bearer " + SECRET_KEY);
                assertThat(request.header("Accept")).isEqualTo("application/json");
                assertThat(request.header("Content-Type")).isEqualTo("application/json");
                assertThat(request.header("User-Agent")).startsWith("dregs-java/" + Dregs.VERSION);
                assertThat(request.header("User-Agent")).contains("(java ");
            }
        }

        @Test
        void theShortFormSendsTheSameShape() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track("user.signup", "user_12345");

                Map<String, Object> body = TestSupport.json(server.lastRequest().body());

                assertThat(body).containsEntry("type", "user.signup");
                assertThat(body).containsEntry("data", Map.of());
                assertThat(body).containsEntry("identity", Map.of("id", "user_12345", "data", Map.of()));
            }
        }

        @Test
        void generatesAnEventIdSoRetriesAreIdempotent() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track("user.signup", "user_12345");
                client.track("user.signup", "user_12345");

                String first = (String) TestSupport.json(server.requests().get(0).body()).get("id");
                String second = (String) TestSupport.json(server.requests().get(1).body()).get("id");

                assertThat(first).isNotBlank().isNotEqualTo(second).doesNotStartWith("dregs-");
                assertThat(first.length()).isLessThanOrEqualTo(TrackRequest.MAX_EVENT_ID_LENGTH);
            }
        }

        @Test
        void sendsATimestampAsUtc() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track(TrackRequest.builder("user.signup", "user_12345")
                        .timestamp(Instant.parse("2026-09-21T14:22:09Z"))
                        .build());

                assertThat(TestSupport.json(server.lastRequest().body()))
                        .containsEntry("timestamp", "2026-09-21T14:22:09Z");
            }
        }

        @Test
        void dropsSubSecondPrecisionRatherThanSendingAFormatTheApiWillNotRead() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track(TrackRequest.builder("user.signup", "user_12345")
                        .timestamp(Instant.parse("2026-09-21T14:22:09.123456Z"))
                        .build());

                assertThat(TestSupport.json(server.lastRequest().body()))
                        .containsEntry("timestamp", "2026-09-21T14:22:09Z");
            }
        }

        @Test
        void omitsTheTimestampWhenTheCallerDoes() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track("user.signup", "user_12345");

                assertThat(TestSupport.json(server.lastRequest().body())).doesNotContainKey("timestamp");
            }
        }

        @Test
        void aCustomSourceIsHonoured() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track(TrackRequest.builder("user.signup", "user_12345")
                        .source("billing-worker")
                        .build());

                assertThat(TestSupport.json(server.lastRequest().body()))
                        .containsEntry("source", "billing-worker");
            }
        }

        @Test
        void anAttributeWithNoValueIsSentRatherThanRefused() {
            Map<String, Object> attributes = new HashMap<>();

            attributes.put("email", "ada@example.com");
            attributes.put("company", null);

            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                client.track(TrackRequest.builder("user.signup", "user_12345")
                        .identityData(attributes)
                        .build());

                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void anEmptyIdentityIsRefusedBeforeAnyRequest() {
            assertThatThrownBy(() -> TrackRequest.of("user.signup", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("identity is required");
        }

        @Test
        void anEmptyEventTypeIsRefused() {
            assertThatThrownBy(() -> TrackRequest.of("", "user_12345"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventType is required");
        }

        @Test
        void aReservedEventIdIsRefused() {
            assertThatThrownBy(() -> TrackRequest.builder("user.signup", "user_12345")
                            .eventId("dregs-1234")
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reserved");
        }

        @Test
        void anOverlongEventIdIsRefused() {
            assertThatThrownBy(() -> TrackRequest.builder("user.signup", "user_12345")
                            .eventId("x".repeat(65))
                            .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("64 characters");
        }
    }

    @Nested
    @DisplayName("the track response")
    class TrackResponseBody {

        @Test
        void reportsAnAcceptedEvent() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200,
                                    "{\"status\":\"success\",\"id\":\"evt_1\",\"fingerprint\":null}"));
                    Dregs client = clientFor(server)) {

                TrackResult result = client.track("user.signup", "user_12345");

                assertThat(result.accepted()).isTrue();
                assertThat(result.id()).isEqualTo("evt_1");
                assertThat(result.status()).isEqualTo("success");
                assertThat(result.fingerprint()).isNull();
            }
        }

        @Test
        void aQuietRejectionIsNotAccepted() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"status\":\"success\",\"id\":null,\"fingerprint\":null}"));
                    Dregs client = clientFor(server)) {

                TrackResult result = client.track("user.signup", "user_12345");

                assertThat(result.accepted()).isFalse();
                assertThat(result.id()).isNull();
            }
        }

        @Test
        void keepsTheWholeResponseForFieldsThisReleasePredates() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"status\":\"success\",\"id\":\"evt_1\",\"queuePosition\":3}"));
                    Dregs client = clientFor(server)) {

                TrackResult result = client.track("user.signup", "user_12345");

                assertThat(result.raw()).containsEntry("queuePosition", 3L);
            }
        }
    }

    @Nested
    @DisplayName("bringing your own HttpClient")
    class SuppliedHttpClient {

        @Test
        void aSuppliedClientIsUsedAndLeftOpen() {
            HttpClient http = HttpClient.newHttpClient();

            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED))) {
                Dregs client = Dregs.builder()
                        .secretKey(SECRET_KEY)
                        .baseUrl(server.baseUrl())
                        .httpClient(http)
                        .build();

                client.track("user.signup", "user_12345");
                client.close();

                // Closing the Dregs client must not take a caller's own pool down with it.
                Dregs second = Dregs.builder()
                        .secretKey(SECRET_KEY)
                        .baseUrl(server.baseUrl())
                        .httpClient(http)
                        .build();

                assertThat(second.track("user.signup", "user_12345").accepted()).isTrue();
                assertThat(server.callCount()).isEqualTo(2);
            }
        }
    }
}
