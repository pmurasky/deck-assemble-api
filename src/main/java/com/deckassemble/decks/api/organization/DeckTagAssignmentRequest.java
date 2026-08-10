package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Bulk-replace body: the full set of tag ids now assigned to this deck. */
public record DeckTagAssignmentRequest(
        @NotNull List<Long> tagIds, @Nullable Integer expectedRevision) {}
