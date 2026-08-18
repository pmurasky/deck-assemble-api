package com.deckassemble.decks.application.simulation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Starts a solitaire turn-stepped practice session from a deck revision's snapshot: an opening
 * hand (same mulligan mechanics as {@link DeckSimulationRequest}) is kept, then each step advances
 * one turn of draw / land-drop / cast. {@code onThePlay} skips the turn-1 draw. {@code seed} is
 * optional; if omitted, one is generated and returned so the session is reproducible.
 */
public record PracticeSessionRequest(
        @NotNull Integer revision,
        @NotNull Boolean onThePlay,
        @NotNull MulliganStrategy mulliganStrategy,
        @Nullable @Min(0) Integer minimumLands,
        @Nullable @Min(0) Integer maximumLands,
        @Nullable Long seed)
        implements MulliganRequest {}
