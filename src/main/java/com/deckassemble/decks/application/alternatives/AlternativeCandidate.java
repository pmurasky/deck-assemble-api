package com.deckassemble.decks.application.alternatives;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import com.deckassemble.recommendations.application.CardScore;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** A candidate card with the ownership, price, and combo context used for ranking. */
record AlternativeCandidate(
        long printingId,
        Card card,
        Category category,
        CardScore score,
        boolean owned,
        @Nullable BigDecimal priceUsd,
        boolean breaksCombo) {}
