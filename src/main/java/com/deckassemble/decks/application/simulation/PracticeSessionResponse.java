package com.deckassemble.decks.application.simulation;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Snapshot of one player-directed solitaire practice session. */
public record PracticeSessionResponse(
        UUID sessionId,
        long seed,
        int turn,
        int mulliganCount,
        List<CardView> hand,
        @Nullable CardView drawnCard,
        List<CardView> castableSpells,
        boolean finished,
        List<PermanentView> battlefield) {

    public record CardView(
            long printingId,
            String name,
            @Nullable String imageUrl,
            @Nullable String manaCost,
            @Nullable String typeLine,
            @Nullable String oracleText) {}

    public record PermanentView(long printingId, CardView card, boolean tapped) {}
}
