package com.deckassemble.decks.domain.history;

/** Categorizes the user-visible mutation that produced a {@link DeckRevision}. */
public enum DeckChangeType {
    CREATED,
    METADATA_UPDATED,
    COMMANDER_CHANGED,
    CARD_ADDED,
    CARD_UPDATED,
    CARD_REMOVED,
    CATEGORY_CHANGED,
    TAG_CHANGED,
    FOLDER_CHANGED,
    IMPORTED,
    RESTORED
}
