package com.deckassemble.decks.application;

import com.deckassemble.decks.domain.DeckCard;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

/**
 * Partial update: a {@code null} field is left unchanged (see {@code DeckCardService.updateCard}).
 */
public record DeckCardUpdateRequest(
        @Min(1) @Max(9999) @Nullable Integer quantity, DeckCard.@Nullable Section deckSection) {}
