package com.deckassemble.decks.api;

import com.deckassemble.decks.domain.DeckCard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DeckCardUpdateRequest(
    @Min(1) @Max(9999) Integer quantity,
    DeckCard.Section deckSection) {
}
