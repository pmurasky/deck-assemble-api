package com.deckassemble.decks.api.publishing;

/** Owner-controlled toggle for whether new comments may be posted on a deck's shared view. */
public record DeckCommentsEnabledRequest(boolean enabled) {}
