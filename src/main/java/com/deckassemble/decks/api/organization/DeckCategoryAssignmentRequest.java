package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Bulk-replace body: the full set of deck card ids now assigned to this category. */
public record DeckCategoryAssignmentRequest(
        @NotNull List<Long> deckCardIds, @Nullable Integer expectedRevision) {}
