package com.deckassemble.decks.application.simulation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregate Monte Carlo consistency statistics for one deck revision, plus the seed that produced
 * them (the same seed against the same revision and request reproduces byte-identical output).
 * Every {@code ...ByTurn} map is keyed by turn number (1-based, through the request's {@code
 * turns}). Statistical goldfishing only — no card-text execution.
 */
// Justified: method-local maps built while copying into an immutable record, never shared across
// threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public record DeckSimulationResponse(
        long seed,
        int iterations,
        int turns,
        Map<Integer, Double> landDropProbabilityByTurn,
        Map<String, Map<Integer, Double>> colorAvailabilityByTurn,
        Map<Integer, Double> cardsSeenByTurn,
        Map<Integer, Double> castabilityByTurn,
        Map<Integer, Double> playableSpellCountByTurn,
        ConfidenceMetadata confidence) {

    public DeckSimulationResponse {
        landDropProbabilityByTurn = byTurnCopy(landDropProbabilityByTurn);
        colorAvailabilityByTurn = copyOfNested(colorAvailabilityByTurn);
        cardsSeenByTurn = byTurnCopy(cardsSeenByTurn);
        castabilityByTurn = byTurnCopy(castabilityByTurn);
        playableSpellCountByTurn = byTurnCopy(playableSpellCountByTurn);
    }

    // Map.copyOf does not preserve iteration order; these maps are keyed by turn number and are
    // far more useful to callers (and tests) in ascending turn order, so wrap in a TreeMap instead
    // (same ordering technique ManaCurveCalculator/ManaProductionCalculator already use).
    private static Map<Integer, Double> byTurnCopy(Map<Integer, Double> byTurn) {
        return Collections.unmodifiableMap(new TreeMap<>(byTurn));
    }

    private static Map<String, Map<Integer, Double>> copyOfNested(
            Map<String, Map<Integer, Double>> nested) {
        Map<String, Map<Integer, Double>> copy = new LinkedHashMap<>();
        nested.forEach((color, byTurn) -> copy.put(color, byTurnCopy(byTurn)));
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Monte Carlo confidence: the 95%-confidence margin of error for any proportion-style statistic
     * in this response (land drop, color availability, castability), computed for the worst case (p
     * = 0.5, which maximizes sampling variance) via the standard normal approximation {@code 1.96 *
     * sqrt(0.25 / iterations)}. A simple, documented bound rather than a per-statistic confidence
     * interval — every reported percentage is at least this precise.
     */
    public record ConfidenceMetadata(int iterations, double marginOfErrorPercent95) {

        // 95%-confidence z-score under the standard normal approximation.
        private static final double Z_SCORE_95 = 1.96;
        // p(1-p) at its maximum (p = 0.5): the worst-case sampling variance for any proportion.
        private static final double MAX_PROPORTION_VARIANCE = 0.25;
        private static final double PERCENT = 100;

        public static ConfidenceMetadata of(int iterations) {
            double marginOfError = Z_SCORE_95 * Math.sqrt(MAX_PROPORTION_VARIANCE / iterations);
            return new ConfidenceMetadata(iterations, marginOfError * PERCENT);
        }
    }
}
