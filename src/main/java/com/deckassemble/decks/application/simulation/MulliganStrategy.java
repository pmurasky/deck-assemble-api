package com.deckassemble.decks.application.simulation;

/** How a drawn opening hand is adjusted before it's returned. */
public enum MulliganStrategy {

    /** Keep the first 7 cards drawn, unconditionally. */
    NONE,

    /**
     * London mulligan: draw a fresh 7-card hand until its land count falls within {@code
     * [minimumLands, maximumLands]} (or a small attempt cap is hit), then bottom one card per
     * mulligan taken.
     */
    LONDON_LAND_RANGE
}
