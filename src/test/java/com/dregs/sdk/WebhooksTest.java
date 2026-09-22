package com.dregs.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dregs.sdk.exception.WebhookVerificationException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Webhook signature verification. */
class WebhooksTest {

    private static final String SECRET = "whsec_abc123";
    private static final Instant SENT_AT = Instant.parse("2026-09-21T14:22:09Z");

    private static byte[] payload() {
        return payload("\"user_12345\"");
    }

    private static byte[] payload(String identityId) {
        return ("{\"event\":\"ESCALATION_CREATED\",\"timestamp\":\"2026-09-21T14:22:09Z\","
                        + "\"identityId\":" + identityId + "}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String sign(byte[] body) {
        return Webhooks.computeSignature(body, SECRET);
    }

    @Nested
    @DisplayName("computeSignature")
    class ComputeSignature {

        @Test
        void isAHexEncodedHmacSha256() {
            String signature = sign(payload());

            assertThat(signature).hasSize(64).matches("[0-9a-f]{64}");
        }

        @Test
        void isStableForTheSameBytes() {
            assertThat(sign(payload())).isEqualTo(sign(payload()));
        }

        @Test
        void acceptsAStringBody() {
            byte[] body = payload();

            assertThat(Webhooks.computeSignature(new String(body, StandardCharsets.UTF_8), SECRET))
                    .isEqualTo(Webhooks.computeSignature(body, SECRET));
        }

        @Test
        void changesWithTheSecret() {
            assertThat(Webhooks.computeSignature(payload(), "whsec_other"))
                    .isNotEqualTo(sign(payload()));
        }
    }

    @Nested
    @DisplayName("verifySignature")
    class VerifySignature {

        @Test
        void aGoodSignatureVerifies() {
            assertThat(Webhooks.verifySignature(payload(), sign(payload()), SECRET)).isTrue();
        }

        @Test
        void surroundingWhitespaceIsTolerated() {
            assertThat(Webhooks.verifySignature(payload(), "  " + sign(payload()) + "\n", SECRET))
                    .isTrue();
        }

        @Test
        void aTamperedBodyFails() {
            String signature = sign(payload());

            assertThat(Webhooks.verifySignature(payload("\"someone_else\""), signature, SECRET))
                    .isFalse();
        }

        @Test
        void theWrongSecretFails() {
            String signature = Webhooks.computeSignature(payload(), "whsec_other");

            assertThat(Webhooks.verifySignature(payload(), signature, SECRET)).isFalse();
        }

        @Test
        void anEmptySignatureFails() {
            assertThat(Webhooks.verifySignature(payload(), "", SECRET)).isFalse();
        }

        @Test
        void anEmptySecretFails() {
            assertThat(Webhooks.verifySignature(payload(), sign(payload()), "")).isFalse();
        }

        @Test
        void aNullSignatureFails() {
            assertThat(Webhooks.verifySignature(payload(), null, SECRET)).isFalse();
        }

        @Test
        void aStringBodyVerifiesTheSameWay() {
            byte[] body = payload();

            assertThat(Webhooks.verifySignature(
                            new String(body, StandardCharsets.UTF_8), sign(body), SECRET))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        void returnsTheParsedEvent() {
            Map<String, Object> event =
                    Webhooks.verify(payload(), sign(payload()), SECRET, Webhooks.DEFAULT_TOLERANCE, SENT_AT);

            assertThat(event).containsEntry("event", "ESCALATION_CREATED");
            assertThat(event).containsEntry("identityId", "user_12345");
        }

        @Test
        void aBadSignatureRaises() {
            assertThatThrownBy(() -> Webhooks.verify(payload(), "deadbeef", SECRET))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("signature");
        }

        @Test
        void aBodyThatIsNotJsonRaises() {
            byte[] body = "not json at all".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> Webhooks.verify(body, sign(body), SECRET))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("JSON");
        }

        @Test
        void aJsonArrayBodyRaises() {
            byte[] body = "[1, 2, 3]".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> Webhooks.verify(body, sign(body), SECRET))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("JSON object");
        }

        @Test
        void aStalePayloadIsRefusedAsAReplay() {
            byte[] body = payload();

            assertThatThrownBy(() -> Webhooks.verify(
                            body,
                            sign(body),
                            SECRET,
                            Webhooks.DEFAULT_TOLERANCE,
                            SENT_AT.plus(Duration.ofHours(1))))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("replay");
        }

        @Test
        void aPayloadFromTheFutureIsAlsoRefused() {
            byte[] body = payload();

            assertThatThrownBy(() -> Webhooks.verify(
                            body,
                            sign(body),
                            SECRET,
                            Webhooks.DEFAULT_TOLERANCE,
                            SENT_AT.minus(Duration.ofHours(1))))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("replay");
        }

        @Test
        void aPayloadInsideTheToleranceIsAccepted() {
            byte[] body = payload();

            Map<String, Object> event = Webhooks.verify(
                    body,
                    sign(body),
                    SECRET,
                    Webhooks.DEFAULT_TOLERANCE,
                    SENT_AT.plus(Duration.ofSeconds(120)));

            assertThat(event).containsEntry("event", "ESCALATION_CREATED");
        }

        @Test
        void theFreshnessCheckCanBeWaived() {
            byte[] body = payload();

            Map<String, Object> event = Webhooks.verify(body, sign(body), SECRET, null);

            assertThat(event).containsEntry("event", "ESCALATION_CREATED");
        }

        @Test
        void aMissingTimestampRaisesUnlessTheCheckIsWaived() {
            byte[] body = "{\"event\":\"ESCALATION_CREATED\"}".getBytes(StandardCharsets.UTF_8);
            String signature = sign(body);

            assertThatThrownBy(() -> Webhooks.verify(body, signature, SECRET))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("no timestamp");

            assertThat(Webhooks.verify(body, signature, SECRET, null))
                    .containsEntry("event", "ESCALATION_CREATED");
        }

        @Test
        void anUnreadableTimestampRaises() {
            byte[] body =
                    ("{\"event\":\"ESCALATION_CREATED\",\"timestamp\":\"the day before yesterday\"}")
                            .getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> Webhooks.verify(body, sign(body), SECRET))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("unreadable");
        }

        @Test
        void aTimestampWithAnOffsetIsUnderstood() {
            byte[] body =
                    ("{\"event\":\"ESCALATION_CREATED\",\"timestamp\":\"2026-09-21T16:22:09+02:00\"}")
                            .getBytes(StandardCharsets.UTF_8);

            Map<String, Object> event =
                    Webhooks.verify(body, sign(body), SECRET, Webhooks.DEFAULT_TOLERANCE, SENT_AT);

            assertThat(event).containsEntry("event", "ESCALATION_CREATED");
        }

        @Test
        void theDefaultToleranceIsFiveMinutes() {
            assertThat(Webhooks.DEFAULT_TOLERANCE).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        void theHeaderNamesAreTheOnesDregsSends() {
            assertThat(Webhooks.SIGNATURE_HEADER).isEqualTo("X-Dregs-Signature");
            assertThat(Webhooks.TIMESTAMP_HEADER).isEqualTo("X-Dregs-Timestamp");
            assertThat(Webhooks.EVENT_HEADER).isEqualTo("X-Dregs-Event");
        }

        @Test
        void aReserializedBodyDoesNotVerify() {
            byte[] received = payload();
            String signature = sign(received);

            // Key order and whitespace change under re-serialization, and the hash changes with
            // them. This is the mistake the documentation is loudest about.
            byte[] reserialized =
                    ("{ \"identityId\": \"user_12345\", \"event\": \"ESCALATION_CREATED\", "
                                    + "\"timestamp\": \"2026-09-21T14:22:09Z\" }")
                            .getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> Webhooks.verify(reserialized, signature, SECRET))
                    .isInstanceOf(WebhookVerificationException.class)
                    .hasMessageContaining("raw request body");
        }
    }
}
