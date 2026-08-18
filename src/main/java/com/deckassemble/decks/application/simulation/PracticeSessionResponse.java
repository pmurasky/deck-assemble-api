package com.deckassemble.decks.application.simulation;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Snapshot of one solitaire practice session, returned on start, every stepped turn, and reset.
 * {@code drawnCard} and {@code landPlayed} describe the turn just stepped ({@code null} on
 * start/reset, and {@code drawnCard} is {@code null} for turn 1 on the play or when the library is
 * exhausted). {@code castableSpells} lists the in-hand spells whose mana value the lands in play
 * can cover (the same mana-value-only proxy as {@link CastabilityCalculator} — no color checks, no
 * card-text execution). {@code finished} is true once the library has no cards left to draw.
 */
public record PracticeSessionResponse(
        UUID sessionId,
        long seed,
        int turn,
        int mulliganCount,
        List<String> hand,
        @Nullable String drawnCard,
        @Nullable String landPlayed,
        int landsInPlay,
        List<String> castableSpells,
        boolean finished) {}
