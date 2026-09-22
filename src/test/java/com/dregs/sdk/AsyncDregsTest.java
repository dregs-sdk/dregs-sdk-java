package com.dregs.sdk;

import static com.dregs.sdk.TestSupport.SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dregs.sdk.exception.NotFoundException;
import com.dregs.sdk.exception.QuotaExceededException;
import com.dregs.sdk.exception.ServerException;
import com.dregs.sdk.model.Analysis;
import com.dregs.sdk.model.Category;
import com.dregs.sdk.model.Scores;
import com.dregs.sdk.model.TrackResult;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** The async client behaves exactly as the sync one. */
class AsyncDregsTest {

    private static final String ACCEPTED = "{\"status\":\"success\",\"id\":\"evt_1\"}";

    private static AsyncDregs clientFor(StubServer server, int maxRetries) {
        return AsyncDregs.builder()
                .secretKey(SECRET_KEY)
                .baseUrl(server.baseUrl())
                .maxRetries(maxRetries)
                .backoff(TestSupport.noWaiting())
                .build();
    }

    @Test
    void trackSendsTheSameBody() {
        try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                AsyncDregs client = clientFor(server, 0)) {

            TrackResult result = client.track(TrackRequest.builder("user.signup", "user_12345")
                            .data(Map.of("plan", "pro"))
                            .eventId("signup-991")
                            .build())
                    .join();

            Map<String, Object> body = TestSupport.json(server.lastRequest().body());

            assertThat(body).containsEntry("id", "signup-991");
            assertThat(body).containsEntry("source", "java-sdk");
            assertThat(TestSupport.asMap(body.get("identity"))).containsEntry("id", "user_12345");
            assertThat(result.accepted()).isTrue();
        }
    }

    @Test
    void scoresAreParsed() {
        try (StubServer server = new StubServer()
                        .always(StubServer.Reply.json(
                                200,
                                "[{\"category\":\"HUMANITY\",\"value\":85},"
                                        + "{\"category\":\"BEHAVIOR\",\"value\":68}]"));
                AsyncDregs client = clientFor(server, 0)) {

            Scores scores = client.identities().scores("user_12345").join();

            assertThat(scores.humanity()).isEqualTo(85);
            assertThat(scores.behavior()).isEqualTo(68);
            assertThat(scores.items()).extracting(score -> score.category())
                    .containsExactly(Category.HUMANITY, Category.BEHAVIOR);
        }
    }

    @Test
    void analysisIsParsed() {
        try (StubServer server = new StubServer()
                        .always(StubServer.Reply.json(
                                200,
                                """
                                {
                                  "id": 2000871,
                                  "identityId": "user_12345",
                                  "scores": [
                                    {
                                      "category": "HUMANITY",
                                      "value": 85,
                                      "observations": [
                                        {"category": "HUMANITY", "id": "humanity.user-agent"}
                                      ]
                                    }
                                  ]
                                }
                                """));
                AsyncDregs client = clientFor(server, 0)) {

            Analysis analysis = client.identities().analysis("user_12345").join();

            assertThat(analysis.id()).isEqualTo(2000871L);
            assertThat(analysis.observations().get(0).id()).isEqualTo("humanity.user-agent");
        }
    }

    @Test
    void getAndAnalyzeReachTheirEndpoints() {
        try (StubServer server = new StubServer()
                        .enqueue(StubServer.Reply.json(200, "{\"id\":\"user_12345\"}"))
                        .enqueue(StubServer.Reply.status(201));
                AsyncDregs client = clientFor(server, 0)) {

            assertThat(client.identities().get("user_12345").join().id()).isEqualTo("user_12345");
            assertThat(client.identities().analyze("user_12345").join()).isNull();

            assertThat(server.requests()).extracting(StubServer.Recorded::path)
                    .containsExactly(
                            "/api/identities/user_12345", "/api/identities/user_12345/actions/analyze");
        }
    }

    @Test
    void anIdentityIdIsEscapedTheSameWay() {
        try (StubServer server = new StubServer().always(StubServer.Reply.json(200, "{}"));
                AsyncDregs client = clientFor(server, 0)) {

            client.identities().get("ada@example.com").join();

            assertThat(server.lastRequest().path()).isEqualTo("/api/identities/ada%40example.com");
        }
    }

    @Test
    void errorsArriveAsTheFuturesCause() {
        try (StubServer server = new StubServer().always(StubServer.Reply.status(404));
                AsyncDregs client = clientFor(server, 0)) {

            assertThatThrownBy(() -> client.identities().get("nobody").join())
                    .hasCauseInstanceOf(NotFoundException.class);
        }
    }

    @Test
    void aQuotaErrorIsRaisedFromABodyStatus() {
        try (StubServer server = new StubServer()
                        .always(StubServer.Reply.json(200, "{\"status\":\"quota_exceeded\",\"id\":null}"));
                AsyncDregs client = clientFor(server, 0)) {

            assertThatThrownBy(() -> client.track("user.signup", "user_12345").join())
                    .hasCauseInstanceOf(QuotaExceededException.class);
        }
    }

    @Test
    void aServerErrorIsRetried() {
        try (StubServer server = new StubServer()
                        .enqueue(StubServer.Reply.status(503))
                        .always(StubServer.Reply.json(200, "{\"id\":\"user_12345\"}"));
                AsyncDregs client = clientFor(server, 2)) {

            assertThat(client.identities().get("user_12345").join().id()).isEqualTo("user_12345");
            assertThat(server.callCount()).isEqualTo(2);
        }
    }

    @Test
    void retriesStopAtTheConfiguredLimit() {
        try (StubServer server = new StubServer().always(StubServer.Reply.status(500));
                AsyncDregs client = clientFor(server, 2)) {

            assertThatThrownBy(() -> client.identities().get("user_12345").join())
                    .hasCauseInstanceOf(ServerException.class);

            assertThat(server.callCount()).isEqualTo(3);
        }
    }

    @Test
    void aRetriedEventKeepsItsId() {
        try (StubServer server = new StubServer()
                        .enqueue(StubServer.Reply.status(503))
                        .always(StubServer.Reply.json(200, ACCEPTED));
                AsyncDregs client = clientFor(server, 2)) {

            client.track("user.signup", "user_12345").join();

            assertThat(TestSupport.json(server.requests().get(0).body()).get("id"))
                    .isEqualTo(TestSupport.json(server.requests().get(1).body()).get("id"));
        }
    }

    @Test
    void aBadRequestIsRefusedBeforeTheFutureIsEvenCreated() {
        try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                AsyncDregs client = clientFor(server, 0)) {

            assertThatThrownBy(() -> client.track("user.signup", ""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(server.callCount()).isZero();
        }
    }

    @Test
    void theTwoClientsExposeTheSameSurface() {
        assertThat(publicMethodsOf(AsyncDregs.class)).isEqualTo(publicMethodsOf(Dregs.class));
        assertThat(publicMethodsOf(AsyncIdentities.class)).isEqualTo(publicMethodsOf(Identities.class));
    }

    private static Set<String> publicMethodsOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .map(method -> method.getName() + "/" + method.getParameterCount())
                .collect(Collectors.toSet());
    }
}
