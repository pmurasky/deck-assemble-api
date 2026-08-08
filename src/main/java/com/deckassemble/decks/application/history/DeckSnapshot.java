package com.deckassemble.decks.application.history;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Complete, immutable canonical state of a deck at one point in time, serialized as JSON and stored
 * on a {@code DeckRevision}. Captures everything a restore needs to reproduce the deck.
 */
public record DeckSnapshot(
        String name,
        String formatCode,
        @Nullable String description,
        @Nullable Long commanderCardId,
        @Nullable Long secondaryCommanderCardId,
        @Nullable Long folderId,
        boolean useOwnedCardsOnly,
        @Nullable BigDecimal budgetLimit,
        @Nullable Integer desiredPowerLevel,
        @Nullable String playStyle,
        String status,
        List<CardEntry> cards,
        List<String> categoryNames,
        List<String> tagNames) {

    public DeckSnapshot {
        cards = List.copyOf(cards);
        categoryNames = List.copyOf(categoryNames);
        tagNames = List.copyOf(tagNames);
    }

    /** One deck card row within the snapshot. */
    public record CardEntry(
            Long cardPrintingId, int quantity, String deckSection, String ownershipStatus) {}
}
