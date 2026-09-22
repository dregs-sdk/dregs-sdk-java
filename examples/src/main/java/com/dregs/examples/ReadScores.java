package com.dregs.examples;

import com.dregs.Dregs;
import com.dregs.model.Analysis;
import com.dregs.model.Observation;
import com.dregs.model.Scores;
import com.dregs.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Read an identity's scores, and the observations behind them.
 *
 * <pre>
 * DREGS_SECRET_KEY=sk_... ./gradlew runReadScores --args=user_12345
 * </pre>
 */
public final class ReadScores {

    private ReadScores() {
    }

    /**
     * Runs the example.
     *
     * @param args the identity id to read, defaulting to {@code user_12345}
     */
    public static void main(String[] args) {
        String identityId = args.length > 0 ? args[0] : "user_12345";

        try (Dregs client = Dregs.builder().build()) {
            Scores scores;

            try {
                scores = client.identities().scores(identityId);
            } catch (NotFoundException unknown) {
                System.out.println("Dregs has never seen " + identityId + ".");

                return;
            }

            if (scores.isEmpty()) {
                System.out.println(
                        identityId + " has not been scored yet. Scoring runs shortly after new activity.");

                return;
            }

            System.out.println("Scores for " + identityId);
            System.out.println("  Humanity:     " + format(scores.humanity()));
            System.out.println("  Authenticity: " + format(scores.authenticity()));
            System.out.println("  Uniqueness:   " + format(scores.uniqueness()));
            System.out.println("  Behavior:     " + format(scores.behavior()));

            // The scores are the summary. The observations are the evidence, and they come from
            // the analysis cycle rather than from the scores endpoint.
            Analysis analysis;

            try {
                analysis = client.identities().analysis(identityId);
            } catch (NotFoundException notAnalyzed) {
                System.out.println("\nNo analysis cycle has finished for this identity yet.");

                return;
            }

            System.out.println("\nWhy, from the cycle that finished at " + analysis.finishedAt() + ":");

            List<Observation> observations = new ArrayList<>(analysis.observations());

            // Most suspicious first: an observation's value runs from 0.0 (bad) to 1.0 (fine).
            observations.sort(Comparator.comparingDouble(o -> o.value() == null ? 1.0 : o.value()));

            for (Observation observation : observations) {
                System.out.println("  [" + observation.category() + "] " + observation.label());
                System.out.println("      " + observation.explanation());
                System.out.println(
                        "      value " + observation.value() + ", confidence " + observation.confidence());
            }
        }
    }

    private static String format(Integer score) {
        return score == null ? "not scored" : String.valueOf(score);
    }
}
