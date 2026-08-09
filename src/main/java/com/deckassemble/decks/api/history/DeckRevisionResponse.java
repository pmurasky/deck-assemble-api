package com.deckassemble.decks.api.history;

import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** API view of one deck revision, with its snapshot already deserialized. */
public record DeckRevisionResponse(
        int revisionNumber,
        @Nullable Integer baseRevisionNumber,
        String changeType,
        @Nullable String metadata,
        DeckSnapshot snapshot,
        Instant createdAt,
        @Nullable String createdBy) {

    public static DeckRevisionResponse from(DeckRevisionService.RevisionView view) {
        return new DeckRevisionResponse(
                view.revisionNumber(),
                view.baseRevisionNumber(),
                view.changeType().name(),
                view.metadata(),
                view.snapshot(),
                view.createdAt(),
                view.createdBy());
    }
}
