package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Bulk-replace body: the full set of deck card ids now assigned to this category. */
public record DeckCategoryAssignmentRequest(@NotNull List<Long> deckCardIds) {}
