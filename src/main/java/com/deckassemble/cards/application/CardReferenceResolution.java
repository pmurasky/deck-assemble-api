package com.deckassemble.cards.application;

import java.util.List;
import java.util.UUID;

/** Result of resolving an external card reference. */
public sealed interface CardReferenceResolution {

    /** A single exact card printing match. */
    record Matched(UUID cardId, UUID printingId) implements CardReferenceResolution {}

    /** Multiple exact-name printings that require caller disambiguation. */
    record Ambiguous(List<UUID> printingIds) implements CardReferenceResolution {}

    /** No supported exact match. */
    record Unmatched() implements CardReferenceResolution {}
}
