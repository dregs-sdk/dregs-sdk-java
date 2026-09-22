package com.dregs;

import static com.dregs.TestSupport.SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dregs.model.Analysis;
import com.dregs.model.Category;
import com.dregs.model.Identity;
import com.dregs.model.Observation;
import com.dregs.model.Score;
import com.dregs.model.Scores;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The identities namespace and the models it returns. */
class IdentitiesTest {

    private static final String IDENTITY =
            """
            {
              "id": "user_12345",
              "displayName": "Ada Lovelace",
              "displayEmail": "ada@example.com",
              "displayUsername": "ada",
              "humanityScore": 85,
              "authenticityScore": 72,
              "uniquenessScore": 91,
              "behaviorScore": 68,
              "createdAt": "2026-09-01T10:00:00Z",
              "lastTrackedAt": "2026-09-21T14:20:00Z",
              "disregarded": false,
              "badges": [
                {
                  "slug": "behavior.account-takeover-signal",
                  "name": "Account Takeover Suspected",
                  "type": "DANGER",
                  "explanation": "Credential change from a new device."
                }
              ],
              "data": {"email": "ada@example.com", "plan": "pro"}
            }
            """;

    private static final String ANALYSIS =
            """
            {
              "id": 2000871,
              "identityId": "user_12345",
              "scores": [
                {
                  "category": "HUMANITY",
                  "value": 85,
                  "observations": [
                    {
                      "category": "HUMANITY",
                      "id": "humanity.user-agent",
                      "label": "User Agent Analysis",
                      "explanation": "Browser fingerprint consistent with standard Chrome on macOS",
                      "value": 0.92,
                      "confidence": 0.85,
                      "weight": 0.85,
                      "metadata": {"browser": "Chrome"}
                    }
                  ]
                },
                {"category": "BEHAVIOR", "value": 68, "observations": []}
              ],
              "eventCount": 47,
              "deviceCount": 2,
              "durationMillis": 312,
              "startedAt": "2026-09-21T14:22:09Z",
              "finishedAt": "2026-09-21T14:22:09Z"
            }
            """;

    private static Dregs clientFor(StubServer server) {
        return Dregs.builder().secretKey(SECRET_KEY).baseUrl(server.baseUrl()).maxRetries(0).build();
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        void parsesAnIdentity() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, IDENTITY));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities().get("user_12345");

                assertThat(identity.id()).isEqualTo("user_12345");
                assertThat(identity.displayName()).isEqualTo("Ada Lovelace");
                assertThat(identity.displayEmail()).isEqualTo("ada@example.com");
                assertThat(identity.displayUsername()).isEqualTo("ada");
                assertThat(identity.humanityScore()).isEqualTo(85);
                assertThat(identity.disregarded()).isFalse();
                assertThat(identity.data()).containsEntry("plan", "pro");
                assertThat(identity.createdAt()).isEqualTo(Instant.parse("2026-09-01T10:00:00Z"));
                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/user_12345");
            }
        }

        @Test
        void parsesBadges() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, IDENTITY));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities().get("user_12345");

                assertThat(identity.badges()).hasSize(1);
                assertThat(identity.badges().get(0).name()).isEqualTo("Account Takeover Suspected");
                assertThat(identity.badges().get(0).slug())
                        .isEqualTo("behavior.account-takeover-signal");
            }
        }

        @Test
        void exposesTheSameScoresViewAsTheScoresCall() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, IDENTITY));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().get("user_12345").scores();

                assertThat(scores.humanity()).isEqualTo(85);
                assertThat(scores.authenticity()).isEqualTo(72);
                assertThat(scores.uniqueness()).isEqualTo(91);
                assertThat(scores.behavior()).isEqualTo(68);
            }
        }

        @Test
        void anUnscoredCategoryIsAbsentFromTheIdentitysScores() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"id\":\"user_12345\",\"humanityScore\":85}"));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().get("user_12345").scores();

                assertThat(scores.size()).isEqualTo(1);
                assertThat(scores.behavior()).isNull();
            }
        }

        @Test
        void escapesAnIdentityIdThatNeedsIt() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, "{\"id\":\"ada@example.com\"}"));
                    Dregs client = clientFor(server)) {

                client.identities().get("ada@example.com");

                assertThat(server.lastRequest().path())
                        .isEqualTo("/api/identities/ada%40example.com");
            }
        }

        @Test
        void escapesASlashRatherThanLettingItChangeTheEndpoint() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "{}"));
                    Dregs client = clientFor(server)) {

                client.identities().get("tenant/7");

                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/tenant%2F7");
            }
        }

        @Test
        void anEmptyIdIsRefusedBeforeAnyRequest() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "{}"));
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.identities().get(""))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("identity id is required");

                assertThat(server.callCount()).isZero();
            }
        }
    }

    @Nested
    @DisplayName("scores")
    class ScoresCall {

        private static final String ALL_FOUR =
                """
                [
                  {"category": "HUMANITY", "value": 85},
                  {"category": "AUTHENTICITY", "value": 72},
                  {"category": "UNIQUENESS", "value": 91},
                  {"category": "BEHAVIOR", "value": 68}
                ]
                """;

        @Test
        void exposesEachCategoryByName() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ALL_FOUR));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().scores("user_12345");

                assertThat(scores.humanity()).isEqualTo(85);
                assertThat(scores.authenticity()).isEqualTo(72);
                assertThat(scores.uniqueness()).isEqualTo(91);
                assertThat(scores.behavior()).isEqualTo(68);
                assertThat(server.lastRequest().path()).isEqualTo("/api/identities/user_12345/scores");
            }
        }

        @Test
        void behavesAsACollection() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200,
                                    "[{\"category\":\"HUMANITY\",\"value\":85},"
                                            + "{\"category\":\"BEHAVIOR\",\"value\":68}]"));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().scores("user_12345");

                assertThat(scores.size()).isEqualTo(2);
                assertThat(scores.items()).extracting(Score::category)
                        .containsExactly(Category.HUMANITY, Category.BEHAVIOR);
                assertThat(scores).hasSize(2);
                assertThat(scores.items().get(0).value()).isEqualTo(85);
            }
        }

        @Test
        void anUnscoredCategoryReadsAsNull() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, "[{\"category\":\"HUMANITY\",\"value\":85}]"));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().scores("user_12345");

                assertThat(scores.humanity()).isEqualTo(85);
                assertThat(scores.behavior()).isNull();
                assertThat(scores.get(Category.BEHAVIOR)).isNull();
                assertThat(scores.value(Category.BEHAVIOR)).isNull();
            }
        }

        @Test
        void anIdentityWithNoScoresYetIsEmpty() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "[]"));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().scores("user_12345");

                assertThat(scores.isEmpty()).isTrue();
                assertThat(scores.humanity()).isNull();
            }
        }

        @Test
        void carriesNoObservations() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, "[{\"category\":\"HUMANITY\",\"value\":85}]"));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().scores("user_12345");

                assertThat(scores.get(Category.HUMANITY).observations()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("analysis")
    class AnalysisCall {

        @Test
        void parsesACycleAndItsObservations() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ANALYSIS));
                    Dregs client = clientFor(server)) {

                Analysis analysis = client.identities().analysis("user_12345");

                assertThat(analysis.id()).isEqualTo(2000871L);
                assertThat(analysis.identityId()).isEqualTo("user_12345");
                assertThat(analysis.eventCount()).isEqualTo(47);
                assertThat(analysis.deviceCount()).isEqualTo(2);
                assertThat(analysis.durationMillis()).isEqualTo(312L);
                assertThat(analysis.finishedAt()).isEqualTo(Instant.parse("2026-09-21T14:22:09Z"));
                assertThat(analysis.scores().humanity()).isEqualTo(85);
                assertThat(server.lastRequest().path())
                        .isEqualTo("/api/identities/user_12345/analysis");
            }
        }

        @Test
        void carriesTheReasoningBehindEachScore() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ANALYSIS));
                    Dregs client = clientFor(server)) {

                Observation observation =
                        client.identities().analysis("user_12345").scores().items().get(0).observations().get(0);

                assertThat(observation.id()).isEqualTo("humanity.user-agent");
                assertThat(observation.label()).isEqualTo("User Agent Analysis");
                assertThat(observation.value()).isEqualTo(0.92);
                assertThat(observation.confidence()).isEqualTo(0.85);
                assertThat(observation.weight()).isEqualTo(0.85);
                assertThat(observation.metadata()).containsEntry("browser", "Chrome");
            }
        }

        @Test
        void flattensObservationsAcrossCategories() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ANALYSIS));
                    Dregs client = clientFor(server)) {

                Analysis analysis = client.identities().analysis("user_12345");

                assertThat(analysis.observations()).hasSize(1);
                assertThat(analysis.observations().get(0).category()).isEqualTo(Category.HUMANITY);
            }
        }
    }

    @Nested
    @DisplayName("analyze")
    class AnalyzeCall {

        @Test
        void postsToTheActionEndpoint() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(201));
                    Dregs client = clientFor(server)) {

                client.identities().analyze("user_12345");

                StubServer.Recorded request = server.lastRequest();

                assertThat(request.method()).isEqualTo("POST");
                assertThat(request.path()).isEqualTo("/api/identities/user_12345/actions/analyze");
                assertThat(request.body()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("forward compatibility")
    class ForwardCompatibility {

        @Test
        void anUnknownFieldSurvivesOnRaw() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"id\":\"user_12345\",\"somethingNew\":42}"));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities().get("user_12345");

                assertThat(identity.raw()).containsEntry("somethingNew", 42L);
            }
        }

        @Test
        void anUnknownCategoryDoesNotBreakParsing() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200,
                                    "[{\"category\":\"REPUTATION\",\"value\":50},"
                                            + "{\"category\":\"HUMANITY\",\"value\":85}]"));
                    Dregs client = clientFor(server)) {

                Scores scores = client.identities().scores("user_12345");

                assertThat(scores.size()).isEqualTo(2);
                assertThat(scores.humanity()).isEqualTo(85);
                assertThat(scores.items().get(0).category()).isNull();
                assertThat(scores.items().get(0).raw()).containsEntry("category", "REPUTATION");
            }
        }

        @Test
        void aMissingFieldIsNullRatherThanAFailure() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "{}"));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities().get("user_12345");

                assertThat(identity.id()).isNull();
                assertThat(identity.humanityScore()).isNull();
                assertThat(identity.createdAt()).isNull();
                assertThat(identity.badges()).isEmpty();
                assertThat(identity.data()).isEmpty();
                assertThat(identity.disregarded()).isFalse();
            }
        }

        @Test
        void aFieldOfTheWrongShapeIsIgnoredRatherThanThrowing() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200,
                                    "{\"id\":\"user_12345\",\"humanityScore\":\"high\","
                                            + "\"badges\":\"none\",\"createdAt\":\"whenever\"}"));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities().get("user_12345");

                assertThat(identity.id()).isEqualTo("user_12345");
                assertThat(identity.humanityScore()).isNull();
                assertThat(identity.badges()).isEmpty();
                assertThat(identity.createdAt()).isNull();
            }
        }

        @Test
        void anEmptyBodyDoesNotThrow() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(200));
                    Dregs client = clientFor(server)) {

                assertThat(client.identities().get("user_12345").id()).isNull();
                assertThat(client.identities().scores("user_12345").isEmpty()).isTrue();
                assertThat(client.identities().analysis("user_12345").observations()).isEmpty();
            }
        }

        @Test
        void theModelsRefuseToBeChangedUnderTheCaller() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, IDENTITY));
                    Dregs client = clientFor(server)) {

                Identity identity = client.identities().get("user_12345");

                assertThatThrownBy(() -> identity.data().put("plan", "free"))
                        .isInstanceOf(UnsupportedOperationException.class);
                assertThatThrownBy(() -> identity.badges().clear())
                        .isInstanceOf(UnsupportedOperationException.class);
            }
        }
    }
}
