package com.deckassemble.decks.application.simulation;

/**
 * Pure "land drop on curve" statistics: the standard Commander-goldfishing proxy for mana
 * consistency is whether a player has drawn enough lands to make every land drop through a given
 * turn, capped at one land played per turn (no ramp/rules modeling — see {@link
 * DeckSimulationService}). Centralizes that one-land-per-turn assumption so {@link
 * ColorAvailabilityCalculator} and {@link CastabilityCalculator} apply it identically.
 */
final class LandDropCalculator {

    private LandDropCalculator() {}

    /** True when the player could have made every land drop through {@code turn}. */
    static boolean onCurve(int cumulativeLandsSeen, int turn) {
        return cumulativeLandsSeen >= turn;
    }

    /** Lands actually in play by {@code turn}: capped at one land drop per turn. */
    static int landsInPlay(int cumulativeLandsSeen, int turn) {
        return Math.min(cumulativeLandsSeen, turn);
    }
}
