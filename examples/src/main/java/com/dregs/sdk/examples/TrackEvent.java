package com.dregs.sdk.examples;

import com.dregs.sdk.Dregs;
import com.dregs.sdk.TrackRequest;
import com.dregs.sdk.exception.DregsException;
import com.dregs.sdk.exception.QuotaExceededException;
import com.dregs.sdk.exception.RateLimitException;
import com.dregs.sdk.model.TrackResult;
import java.util.Map;

/**
 * Send a backend event to Dregs.
 *
 * <p>Run it with your credential's secret key in the environment:
 *
 * <pre>
 * DREGS_SECRET_KEY=sk_... ./gradlew runTrackEvent
 * </pre>
 */
public final class TrackEvent {

    private TrackEvent() {
    }

    /**
     * Runs the example.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        try (Dregs client = Dregs.builder().build()) {
            TrackResult result = client.track(TrackRequest.builder("user.signup", "user_12345")
                    // Attributes of the event.
                    .data(Map.of("plan", "pro", "referrer", "partner-x"))
                    // Attributes of the user. The analyzers lean on these, so send what you have.
                    .identityData(Map.of(
                            "email", "ada@example.com",
                            "name", "Ada Lovelace",
                            "username", "ada"))
                    // Your own id for the event makes ingestion idempotent: resending this exact
                    // call is a no-op rather than a second signup.
                    .eventId("signup-991")
                    .build());

            if (result.accepted()) {
                System.out.println("Recorded event " + result.id() + ".");
            } else {
                // Dregs answers a few rejections quietly rather than naming the check that failed.
                System.out.println("The event was not recorded (status " + result.status() + ").");
            }
        } catch (QuotaExceededException exceeded) {
            System.out.println("Over the monthly event limit. The event was not recorded.");
        } catch (RateLimitException limited) {
            Double retryAfter = limited.retryAfter();

            System.out.println("Rate limited. Retry after " + (retryAfter == null ? "a moment" : retryAfter) + ".");
        } catch (DregsException failed) {
            System.out.println("Could not reach Dregs: " + failed.getMessage());
        }
    }
}
