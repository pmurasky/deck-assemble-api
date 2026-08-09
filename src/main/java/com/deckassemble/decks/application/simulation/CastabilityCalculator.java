package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.domain.Card;
import java.math.BigDecimal;

/**
 * Mana-value castability proxy: NOT a rules engine. A spell is treated as "castable" on a given
 * turn purely by comparing its mana value against lands assumed in play that turn ({@link
 * LandDropCalculator#landsInPlay}) — no color-requirement checking, no alternative costs, no
 * card-text execution of any kind. {@code manaValueOf(card) <= landsInPlay} is the whole proxy;
 * {@link DeckSimulationService} applies it via a per-turn mana-value histogram (bucketing each seen
 * spell once by {@link #manaValueOf}, then summing buckets up to {@code landsInPlay}) rather than
 * re-testing every seen spell every turn, but the two are equivalent. Mirrors {@code
 * decks.application.analysis.ManaCurveCalculator}'s null-safe mana-value handling, ported to read
 * {@link Card#getManaValue()} directly rather than the analysis package's card view.
 */
final class CastabilityCalculator {

    private CastabilityCalculator() {}

    /** {@code card}'s mana value as a non-negative int; missing values are treated as 0. */
    static int manaValueOf(Card card) {
        BigDecimal manaValue = card.getManaValue();
        return manaValue == null ? 0 : manaValue.intValue();
    }
}
