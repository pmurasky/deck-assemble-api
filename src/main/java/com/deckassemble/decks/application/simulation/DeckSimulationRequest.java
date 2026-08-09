package com.deckassemble.decks.application.simulation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Requests a Monte Carlo consistency simulation of a deck revision's snapshot: repeatedly draws a
 * seeded opening hand (same mulligan mechanics as {@link DeckSampleHandRequest}) and simulates
 * {@code turns} of draws, aggregating land-drop, color-availability, and castability statistics
 * across {@code iterations} independent games. {@code onThePlay} controls whether turn 1 includes a
 * draw step. {@code seed} is optional; if omitted, one is generated and returned so the caller can
 * reproduce the result later.
 */
public record DeckSimulationRequest(
        @NotNull Integer revision,
        @NotNull @Min(100) @Max(100_000) Integer iterations,
        @NotNull @Min(1) @Max(10) Integer turns,
        @NotNull Boolean onThePlay,
        @NotNull MulliganStrategy mulliganStrategy,
        @Nullable @Min(0) Integer minimumLands,
        @Nullable @Min(0) Integer maximumLands,
        @Nullable Long seed)
        implements MulliganRequest {}
