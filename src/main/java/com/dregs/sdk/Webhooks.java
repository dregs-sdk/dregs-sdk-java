package com.dregs.sdk;

import com.dregs.sdk.exception.WebhookVerificationException;
import com.google.gson.JsonSyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifying webhooks Dregs sends you.
 *
 * <p>Dregs signs every webhook with the channel's signing secret: {@code X-Dregs-Signature} is the
 * hex-encoded HMAC-SHA256 of the raw request body.
 *
 * <p><b>Verify against the bytes you received.</b> Not a re-serialized object, not a string your
 * framework rebuilt from a parsed body. Re-serializing changes key order and whitespace, the hash
 * changes with them, and the signature will never match. In Spring MVC that means a
 * {@code @RequestBody byte[]} or {@code String} parameter rather than a mapped type; in a servlet,
 * the raw input stream, read once.
 *
 * <pre>{@code
 * @PostMapping("/webhooks/dregs")
 * ResponseEntity<Void> receive(
 *         @RequestBody byte[] body,
 *         @RequestHeader(Webhooks.SIGNATURE_HEADER) String signature) {
 *
 *     Map<String, Object> event;
 *
 *     try {
 *         event = Webhooks.verify(body, signature, System.getenv("DREGS_WEBHOOK_SECRET"));
 *     } catch (WebhookVerificationException rejected) {
 *         return ResponseEntity.badRequest().build();
 *     }
 *
 *     handle(event);
 *
 *     return ResponseEntity.ok().build();
 * }
 * }</pre>
 *
 * <p>The signing secret is shown once, when you create the webhook channel. It is not your API
 * secret key: one authenticates you to Dregs, the other proves a payload came from Dregs.
 */
public final class Webhooks {

    /** The header carrying the hex-encoded HMAC-SHA256 of the body. */
    public static final String SIGNATURE_HEADER = "X-Dregs-Signature";

    /** The header carrying the delivery's timestamp. */
    public static final String TIMESTAMP_HEADER = "X-Dregs-Timestamp";

    /** The header naming the event type of the delivery. */
    public static final String EVENT_HEADER = "X-Dregs-Event";

    /** How far out of date a webhook's timestamp may be before it is treated as a replay. */
    public static final Duration DEFAULT_TOLERANCE = Duration.ofSeconds(300);

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private Webhooks() {
    }

    /**
     * Returns the hex-encoded HMAC-SHA256 of a payload under a secret.
     *
     * <p>Exposed mostly so a test suite on your side can sign a fixture the way Dregs would.
     *
     * @param payload the raw request body
     * @param secret the channel's signing secret
     * @return the signature, lowercase hex
     */
    public static String computeSignature(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));

            return hex(mac.doFinal(payload));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            // HmacSHA256 is required of every JRE, so this only fires on an empty key.
            throw new WebhookVerificationException(
                    "Could not compute the webhook signature: " + exception.getMessage(), exception);
        }
    }

    /**
     * Returns the hex-encoded HMAC-SHA256 of a payload under a secret.
     *
     * @param payload the raw request body, which must be the text exactly as received
     * @param secret the channel's signing secret
     * @return the signature, lowercase hex
     */
    public static String computeSignature(String payload, String secret) {
        return computeSignature(payload.getBytes(StandardCharsets.UTF_8), secret);
    }

    /**
     * Returns whether a signature matches a payload.
     *
     * <p>The comparison is constant-time. Prefer {@link #verify(byte[], String, String)}, which
     * also rejects replays and hands back the parsed event; reach for this one only when you need
     * the boolean.
     *
     * @param payload the raw request body, exactly as received
     * @param signature the {@code X-Dregs-Signature} header
     * @param secret the channel's signing secret
     * @return true when the signature matches
     */
    public static boolean verifySignature(byte[] payload, String signature, String secret) {
        if (payload == null || signature == null || signature.isEmpty() || secret == null || secret.isEmpty()) {
            return false;
        }

        byte[] expected = computeSignature(payload, secret).getBytes(StandardCharsets.UTF_8);
        byte[] presented = signature.trim().getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, presented);
    }

    /**
     * Returns whether a signature matches a payload.
     *
     * @param payload the raw request body, exactly as received
     * @param signature the {@code X-Dregs-Signature} header
     * @param secret the channel's signing secret
     * @return true when the signature matches
     */
    public static boolean verifySignature(String payload, String signature, String secret) {
        return payload != null
                && verifySignature(payload.getBytes(StandardCharsets.UTF_8), signature, secret);
    }

    /**
     * Verifies a webhook and returns its parsed body.
     *
     * <p>Rejects a payload whose own timestamp is more than {@link #DEFAULT_TOLERANCE} away from
     * now, as a replay.
     *
     * @param payload the raw request body, exactly as received. Not a re-serialized object
     * @param signature the {@code X-Dregs-Signature} header
     * @param secret the channel's signing secret
     * @return the parsed webhook body: {@code event}, {@code timestamp}, and the payload for that
     *     event
     * @throws WebhookVerificationException when the signature did not match, the body was not a
     *     JSON object, or the payload is stale
     */
    public static Map<String, Object> verify(byte[] payload, String signature, String secret) {
        return verify(payload, signature, secret, DEFAULT_TOLERANCE, null);
    }

    /**
     * Verifies a webhook and returns its parsed body.
     *
     * @param payload the raw request body, exactly as received
     * @param signature the {@code X-Dregs-Signature} header
     * @param secret the channel's signing secret
     * @param tolerance how far out of date the payload's own {@code timestamp} may be before it is
     *     treated as a replay. Pass {@code null} to skip the check, which you should only do if
     *     you are deduplicating on the event id yourself. The timestamp is inside the signed body,
     *     so an attacker cannot alter it without breaking the signature
     * @return the parsed webhook body
     * @throws WebhookVerificationException when the signature did not match, the body was not a
     *     JSON object, or the payload is stale
     */
    public static Map<String, Object> verify(
            byte[] payload, String signature, String secret, Duration tolerance) {
        return verify(payload, signature, secret, tolerance, null);
    }

    /**
     * Verifies a webhook and returns its parsed body.
     *
     * @param payload the raw request body, exactly as received
     * @param signature the {@code X-Dregs-Signature} header
     * @param secret the channel's signing secret
     * @param tolerance the replay window, or {@code null} to skip the check
     * @param now the current time, for tests. Pass {@code null} for the clock
     * @return the parsed webhook body
     * @throws WebhookVerificationException when the signature did not match, the body was not a
     *     JSON object, or the payload is stale
     */
    public static Map<String, Object> verify(
            byte[] payload, String signature, String secret, Duration tolerance, Instant now) {
        if (!verifySignature(payload, signature, secret)) {
            throw new WebhookVerificationException(
                    "The webhook signature did not match. Check that you are verifying the raw "
                            + "request body rather than a re-serialized copy, and that the signing "
                            + "secret belongs to the channel that sent this delivery.");
        }

        Object parsed;

        try {
            parsed = Json.parseStrict(new String(payload, StandardCharsets.UTF_8));
        } catch (JsonSyntaxException exception) {
            throw new WebhookVerificationException(
                    "The webhook body was not valid JSON: " + exception.getMessage(), exception);
        }

        if (!(parsed instanceof Map)) {
            throw new WebhookVerificationException("The webhook body was not a JSON object.");
        }

        Map<String, Object> event = Json.asObject(parsed);

        if (tolerance != null) {
            checkFreshness(event, tolerance, now == null ? Instant.now() : now);
        }

        return event;
    }

    /**
     * Verifies a webhook and returns its parsed body.
     *
     * @param payload the raw request body, exactly as received
     * @param signature the {@code X-Dregs-Signature} header
     * @param secret the channel's signing secret
     * @return the parsed webhook body
     * @throws WebhookVerificationException when the signature did not match, the body was not a
     *     JSON object, or the payload is stale
     */
    public static Map<String, Object> verify(String payload, String signature, String secret) {
        return verify(payload.getBytes(StandardCharsets.UTF_8), signature, secret);
    }

    private static void checkFreshness(Map<String, Object> event, Duration tolerance, Instant now) {
        Object raw = event.get("timestamp");

        if (!(raw instanceof String text) || text.isEmpty()) {
            throw new WebhookVerificationException(
                    "The webhook carried no timestamp, so it cannot be checked for replay. Pass a "
                            + "null tolerance if you are deduplicating deliveries some other way.");
        }

        Instant sent = parseTimestamp(text);

        if (sent == null) {
            throw new WebhookVerificationException("The webhook timestamp was unreadable: " + text);
        }

        long age = Math.abs(Duration.between(sent, now).getSeconds());

        if (age > tolerance.getSeconds()) {
            throw new WebhookVerificationException(
                    "The webhook timestamp is " + age + "s away from now, beyond the "
                            + tolerance.getSeconds() + "s tolerance. Treating it as a replay.");
        }
    }

    private static Instant parseTimestamp(String text) {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException stillNot) {
                try {
                    return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
                } catch (DateTimeParseException unreadable) {
                    return null;
                }
            }
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            text.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }

        return text.toString();
    }
}
