package com.deckassemble.decks.application.simulation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Requests one or more seeded sample opening hands from a deck revision's snapshot. {@code
 * minimumLands}/{@code maximumLands} only apply (and are required) when {@code mulliganStrategy} is
 * {@link MulliganStrategy#LONDON_LAND_RANGE}. {@code seed} is optional; if omitted, one is
 * generated and returned so the caller can reproduce the result later.
 */
public record DeckSampleHandRequest(
        @NotNull Integer revision,
        @NotNull @Min(1) @Max(100) Integer handCount,
        @NotNull MulliganStrategy mulliganStrategy,
        @Nullable @Min(0) Integer minimumLands,
        @Nullable @Min(0) Integer maximumLands,
        @Nullable Long seed) {}
