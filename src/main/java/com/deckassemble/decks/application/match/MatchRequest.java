package com.deckassemble.decks.application.match;

import com.deckassemble.decks.application.simulation.MulliganRequest;
import com.deckassemble.decks.application.simulation.MulliganStrategy;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Request to start a two-player Commander match: both deck revisions, one shared mulligan
 * configuration, an optional deterministic seed, and which side is on the play.
 */
public record MatchRequest(
        long yourDeckId,
        @NotNull Integer yourRevision,
        long opponentDeckId,
        @NotNull Integer opponentRevision,
        @NotNull MulliganStrategy mulliganStrategy,
        @Nullable Integer minimumLands,
        @Nullable Integer maximumLands,
        @Nullable Long seed,
        @NotNull Boolean callerOnThePlay)
        implements MulliganRequest {}
