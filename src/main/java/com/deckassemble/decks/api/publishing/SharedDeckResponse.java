package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import org.jspecify.annotations.Nullable;

/**
 * Publishing-facing view of a deck. Once the deck has been published, the content fields (name,
 * formatCode, description, commander(s)) come from the pinned {@link DeckSnapshot} rather than the
 * live {@code Deck} row, so later private edits do not change what's shown here until republished.
 * A never-published deck falls back to live current state (Task 6's original behavior — nothing
 * gates shared-view access on publish state). visibility/shareSlug/primer are always live: the
 * primer is treated as author's-note prose that stays editable independent of publish/republish,
 * not deck content that needs pinning (see DeckPublishingService).
 */
public record SharedDeckResponse(
        long deckId,
        String name,
        String formatCode,
        @Nullable String description,
        @Nullable Long commanderCardId,
        @Nullable Long secondaryCommanderCardId,
        DeckVisibility visibility,
        @Nullable String shareSlug,
        @Nullable String primerTitle,
        @Nullable String primerMarkdown) {

    public static SharedDeckResponse from(Deck deck) {
        return from(deck, null);
    }

    public static SharedDeckResponse from(Deck deck, @Nullable DeckSnapshot pinnedSnapshot) {
        return new SharedDeckResponse(
                deck.getId(),
                pinnedSnapshot != null ? pinnedSnapshot.name() : deck.getName(),
                pinnedSnapshot != null ? pinnedSnapshot.formatCode() : deck.getFormatCode(),
                pinnedSnapshot != null ? pinnedSnapshot.description() : deck.getDescription(),
                pinnedSnapshot != null
                        ? pinnedSnapshot.commanderCardId()
                        : deck.getCommanderCardId(),
                pinnedSnapshot != null
                        ? pinnedSnapshot.secondaryCommanderCardId()
                        : deck.getSecondaryCommanderCardId(),
                deck.getVisibility(),
                deck.getShareSlug(),
                deck.getPrimerTitle(),
                deck.getPrimerMarkdown());
    }
}
