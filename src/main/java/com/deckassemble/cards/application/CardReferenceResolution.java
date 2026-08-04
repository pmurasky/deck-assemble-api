package com.deckassemble.cards.application;

import java.util.List;

/** Result of resolving an external card reference. */
public sealed interface CardReferenceResolution {

    /** A single exact card printing match. */
    record Matched(Long cardId, Long printingId) implements CardReferenceResolution {}

    /** Multiple exact-name printings that require caller disambiguation. */
    record Ambiguous(List<Long> printingIds) implements CardReferenceResolution {}

    /** No supported exact match. */
    record Unmatched() implements CardReferenceResolution {}
}
