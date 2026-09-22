package com.dregs;

import static com.dregs.TestSupport.SECRET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.dregs.exception.AuthenticationException;
import com.dregs.exception.BadRequestException;
import com.dregs.exception.DregsApiException;
import com.dregs.exception.DregsConnectionException;
import com.dregs.exception.DregsException;
import com.dregs.exception.DregsTimeoutException;
import com.dregs.exception.NotFoundException;
import com.dregs.exception.PermissionDeniedException;
import com.dregs.exception.QuotaExceededException;
import com.dregs.exception.RateLimitException;
import com.dregs.exception.ServerException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Statuses map to typed exceptions, and failed requests are retried. */
class ErrorsTest {

    private static final String ACCEPTED = "{\"status\":\"success\",\"id\":\"evt_1\"}";

    private static String errorBody(int status, String message) {
        return "{\"timestamp\":\"2026-09-21T14:22:09Z\",\"status\":" + status
                + ",\"error\":\"Error\",\"message\":\"" + message + "\"}";
    }

    private static Dregs clientFor(StubServer server) {
        return Dregs.builder()
                .secretKey(SECRET_KEY)
                .baseUrl(server.baseUrl())
                .maxRetries(0)
                .backoff(TestSupport.noWaiting())
                .build();
    }

    private static Dregs retryingClientFor(StubServer server) {
        return Dregs.builder()
                .secretKey(SECRET_KEY)
                .baseUrl(server.baseUrl())
                .maxRetries(2)
                .backoff(TestSupport.noWaiting())
                .build();
    }

    @Nested
    @DisplayName("status mapping")
    class StatusMapping {

        @ParameterizedTest(name = "{0} raises {1}")
        @CsvSource({
            "400, com.dregs.exception.BadRequestException",
            "401, com.dregs.exception.AuthenticationException",
            "402, com.dregs.exception.QuotaExceededException",
            "403, com.dregs.exception.PermissionDeniedException",
            "404, com.dregs.exception.NotFoundException",
            "429, com.dregs.exception.RateLimitException",
            "500, com.dregs.exception.ServerException",
            "503, com.dregs.exception.ServerException",
        })
        void eachStatusRaisesItsOwnError(int status, Class<? extends DregsApiException> expected) {
            try (StubServer server =
                            new StubServer().always(StubServer.Reply.json(status, errorBody(status, "No")));
                    Dregs client = clientFor(server)) {

                DregsApiException thrown =
                        catchThrowableOfType(expected, () -> client.identities().get("user_12345"));

                assertThat(thrown).isInstanceOf(expected);
                assertThat(thrown.statusCode()).isEqualTo(status);
            }
        }

        @Test
        void aStatusThisReleaseDoesNotKnowStillRaisesTheBaseClass() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(418, "{}"));
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isExactlyInstanceOf(DregsApiException.class);
            }
        }

        @Test
        void aRedirectIsRefusedRatherThanFollowedWithTheSecretKey() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.status(301)
                                    .withHeader("Location", "https://elsewhere.example/api/identities/x"));
                    Dregs client = clientFor(server)) {

                DregsApiException thrown = catchThrowableOfType(
                        DregsApiException.class, () -> client.identities().get("user_12345"));

                assertThat(thrown.statusCode()).isEqualTo(301);
                assertThat(thrown).hasMessageContaining("does not follow redirects");
                assertThat(thrown).hasMessageContaining("elsewhere.example");

                // One request: the redirect was not chased, so the key never left the host.
                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void everyErrorIsCatchableAsTheBaseClass() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(404));
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(DregsException.class);
            }
        }

        @Test
        void theApiMessageReachesTheException() {
            try (StubServer server =
                            new StubServer().always(StubServer.Reply.json(404, errorBody(404, "Not Found")));
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessage("Not Found")
                        .hasToString("HTTP 404: Not Found");
            }
        }

        @Test
        void aRequestIdIsCarriedThrough() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.status(500).withHeader("X-Request-Id", "req_abc"));
                    Dregs client = clientFor(server)) {

                ServerException thrown = catchThrowableOfType(
                        ServerException.class, () -> client.identities().get("user_12345"));

                assertThat(thrown.requestId()).isEqualTo("req_abc");
                assertThat(thrown).hasToString("HTTP 500: Request failed (request req_abc)");
            }
        }

        @Test
        void aNonJsonErrorStillRaisesTheRightClass() {
            try (StubServer server =
                            new StubServer().always(StubServer.Reply.text(502, "<html>gateway</html>"));
                    Dregs client = clientFor(server)) {

                ServerException thrown = catchThrowableOfType(
                        ServerException.class, () -> client.identities().get("user_12345"));

                assertThat(thrown.body()).isNull();
                assertThat(thrown.statusCode()).isEqualTo(502);
            }
        }

        @Test
        void theParsedBodyIsKeptForTheCallerToInspect() {
            try (StubServer server =
                            new StubServer().always(StubServer.Reply.json(400, errorBody(400, "Bad event")));
                    Dregs client = clientFor(server)) {

                BadRequestException thrown = catchThrowableOfType(
                        BadRequestException.class, () -> client.track("user.signup", "user_12345"));

                assertThat(TestSupport.asMap(thrown.body())).containsEntry("error", "Error");
            }
        }
    }

    @Nested
    @DisplayName("rate limits and quotas")
    class Limits {

        @Test
        void retryAfterIsExposed() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(429, "{\"status\":\"rate_limited\"}")
                                    .withHeader("Retry-After", "2"));
                    Dregs client = clientFor(server)) {

                RateLimitException thrown = catchThrowableOfType(
                        RateLimitException.class, () -> client.track("user.signup", "user_12345"));

                assertThat(thrown.retryAfter()).isEqualTo(2.0);
            }
        }

        @Test
        void anHttpDateRetryAfterFallsBackToTheClientsOwnBackoff() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.status(429)
                                    .withHeader("Retry-After", "Wed, 21 Oct 2026 07:28:00 GMT"));
                    Dregs client = clientFor(server)) {

                RateLimitException thrown = catchThrowableOfType(
                        RateLimitException.class, () -> client.track("user.signup", "user_12345"));

                assertThat(thrown.retryAfter()).isNull();
            }
        }

        @Test
        void aRateLimitReportedInTheBodyStillRaises() {
            // An older API build answered the ingestion limit with HTTP 200 and a body status.
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(200, "{\"status\":\"rate_limited\",\"id\":null}"));
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.track("user.signup", "user_12345"))
                        .isInstanceOf(RateLimitException.class);
            }
        }

        @Test
        void aQuotaReportedInTheBodyStillRaises() {
            try (StubServer server = new StubServer()
                            .always(StubServer.Reply.json(
                                    200, "{\"status\":\"quota_exceeded\",\"id\":null}"));
                    Dregs client = clientFor(server)) {

                QuotaExceededException thrown = catchThrowableOfType(
                        QuotaExceededException.class, () -> client.track("user.signup", "user_12345"));

                assertThat(thrown.statusCode()).isEqualTo(402);
            }
        }

        @Test
        void aQuotaOnTheStatusLineRaisesTheSameThing() {
            try (StubServer server =
                            new StubServer().always(StubServer.Reply.json(402, errorBody(402, "Over limit")));
                    Dregs client = clientFor(server)) {

                assertThatThrownBy(() -> client.track("user.signup", "user_12345"))
                        .isInstanceOf(QuotaExceededException.class);
            }
        }

        @Test
        void anOrdinarySuccessStatusIsNotMistakenForALimit() {
            try (StubServer server = new StubServer().always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = clientFor(server)) {

                assertThat(client.track("user.signup", "user_12345").accepted()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("retries")
    class Retries {

        @Test
        void aServerErrorIsRetriedAndCanSucceed() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.status(503))
                            .always(StubServer.Reply.json(200, "{\"id\":\"user_12345\"}"));
                    Dregs client = retryingClientFor(server)) {

                assertThat(client.identities().get("user_12345").id()).isEqualTo("user_12345");
                assertThat(server.callCount()).isEqualTo(2);
            }
        }

        @Test
        void retriesStopAtTheConfiguredLimit() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(500));
                    Dregs client = retryingClientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(ServerException.class);

                // The first attempt plus two retries.
                assertThat(server.callCount()).isEqualTo(3);
            }
        }

        @Test
        void aRateLimitIsRetried() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.status(429).withHeader("Retry-After", "0"))
                            .always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = retryingClientFor(server)) {

                assertThat(client.track("user.signup", "user_12345").accepted()).isTrue();
                assertThat(server.callCount()).isEqualTo(2);
            }
        }

        @Test
        void aRequestTimeoutIsRetried() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.status(408))
                            .always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = retryingClientFor(server)) {

                assertThat(client.track("user.signup", "user_12345").accepted()).isTrue();
                assertThat(server.callCount()).isEqualTo(2);
            }
        }

        @Test
        void theServersRetryAfterDrivesTheBackoff() {
            TestSupport.RecordingBackoff backoff = new TestSupport.RecordingBackoff();

            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.status(429).withHeader("Retry-After", "3"))
                            .always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = Dregs.builder()
                            .secretKey(SECRET_KEY)
                            .baseUrl(server.baseUrl())
                            .maxRetries(2)
                            .backoff(backoff)
                            .build()) {

                client.track("user.signup", "user_12345");

                assertThat(backoff.retryAfters()).containsExactly(3.0);
            }
        }

        @Test
        void aRetriedEventKeepsItsIdSoIngestionStaysIdempotent() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.status(503))
                            .always(StubServer.Reply.json(200, ACCEPTED));
                    Dregs client = retryingClientFor(server)) {

                client.track("user.signup", "user_12345");

                String first = (String) TestSupport.json(server.requests().get(0).body()).get("id");
                String second = (String) TestSupport.json(server.requests().get(1).body()).get("id");

                assertThat(first).isEqualTo(second);
            }
        }

        @Test
        void aClientErrorIsNotRetried() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(404));
                    Dregs client = retryingClientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(NotFoundException.class);

                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void anAuthenticationFailureIsNotRetried() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(401));
                    Dregs client = retryingClientFor(server)) {

                assertThatThrownBy(() -> client.track("user.signup", "user_12345"))
                        .isInstanceOf(AuthenticationException.class);

                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void aQuotaFailureIsNotRetried() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(402));
                    Dregs client = retryingClientFor(server)) {

                assertThatThrownBy(() -> client.track("user.signup", "user_12345"))
                        .isInstanceOf(QuotaExceededException.class);

                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void aPermissionFailureIsNotRetried() {
            try (StubServer server = new StubServer().always(StubServer.Reply.status(403));
                    Dregs client = retryingClientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(PermissionDeniedException.class);

                assertThat(server.callCount()).isEqualTo(1);
            }
        }

        @Test
        void aConnectionFailureIsRetriedThenRaised() {
            try (StubServer server = new StubServer().always(StubServer.Reply.dropped());
                    Dregs client = retryingClientFor(server)) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(DregsConnectionException.class);

                assertThat(server.callCount()).isGreaterThanOrEqualTo(3);
            }
        }

        @Test
        void aConnectionFailureRecoversWhenARetryLands() {
            try (StubServer server = new StubServer()
                            .enqueue(StubServer.Reply.dropped())
                            .always(StubServer.Reply.json(200, "{\"id\":\"user_12345\"}"));
                    Dregs client = retryingClientFor(server)) {

                assertThat(client.identities().get("user_12345").id()).isEqualTo("user_12345");
            }
        }

        @Test
        void aTimeoutRaisesItsOwnError() {
            try (StubServer server = new StubServer().always(StubServer.Reply.slow(2000));
                    Dregs client = Dregs.builder()
                            .secretKey(SECRET_KEY)
                            .baseUrl(server.baseUrl())
                            .timeout(Duration.ofMillis(150))
                            .maxRetries(0)
                            .backoff(TestSupport.noWaiting())
                            .build()) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(DregsTimeoutException.class)
                        .isInstanceOf(DregsConnectionException.class)
                        .hasMessageContaining("timed out");
            }
        }

        @Test
        void aRefusedConnectionNamesTheUrl() {
            try (Dregs client = Dregs.builder()
                    .secretKey(SECRET_KEY)
                    // Port 1 on loopback: nothing is listening, and nothing will be.
                    .baseUrl("http://127.0.0.1:1/api")
                    .maxRetries(0)
                    .backoff(TestSupport.noWaiting())
                    .build()) {

                assertThatThrownBy(() -> client.identities().get("user_12345"))
                        .isInstanceOf(DregsConnectionException.class)
                        .hasMessageContaining("127.0.0.1:1");
            }
        }
    }
}
