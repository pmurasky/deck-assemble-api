package com.deckassemble.decks.application;

import com.deckassemble.decks.domain.DeckCard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DeckCardAddRequest(
        @NotNull Long cardPrintingId,
        @Min(1) @Max(9999) Integer quantity,
        DeckCard.Section deckSection) {}
