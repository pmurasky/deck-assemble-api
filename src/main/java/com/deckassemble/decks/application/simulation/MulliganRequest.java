package com.deckassemble.decks.application.simulation;

import org.jspecify.annotations.Nullable;

/**
 * The mulligan-strategy fields shared by every request that draws a London-mulligan hand from a
 * deck library ({@link DeckSampleHandRequest}, {@link DeckSimulationRequest}), so {@link
 * MulliganDraw} can operate on either without depending on one concrete request type.
 */
interface MulliganRequest {

    MulliganStrategy mulliganStrategy();

    @Nullable Integer minimumLands();

    @Nullable Integer maximumLands();
}
