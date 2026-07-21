package com.deckassemble.decks.application;

import java.util.List;

public record DeckLegalityResponse(boolean legal, List<Violation> violations) {

    public record Violation(String code, String message) {}
}
