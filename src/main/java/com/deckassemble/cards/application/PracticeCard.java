package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import org.jspecify.annotations.Nullable;

/** Immutable card and printing image retained for a practice session. */
public record PracticeCard(long printingId, Card card, @Nullable String imageUrl) {}
