package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFunctionalCategory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates {@link CardFunctionalCategory#categorize(String, String)} into SQL predicates over
 * {@link Card#getTypeLine()}/{@link Card#getOracleText()}, reusing its marker constants so the
 * search filter and the pure-Java categorizer never drift apart. {@code categorize} returns the
 * first matching category in priority order (LAND, RAMP, DRAW, WIPE, REMOVAL, else SYNERGY); a card
 * matches a requested category only if it hits that category's own markers AND none of the
 * higher-priority categories' markers.
 */
final class CardFunctionalCategoryPredicates {

    private static final List<CardFunctionalCategory> PRIORITY_ORDER =
            List.of(
                    CardFunctionalCategory.LAND,
                    CardFunctionalCategory.RAMP,
                    CardFunctionalCategory.DRAW,
                    CardFunctionalCategory.WIPE,
                    CardFunctionalCategory.REMOVAL);

    private CardFunctionalCategoryPredicates() {}

    static Predicate categoryPredicate(
            From<?, Card> card, CardFunctionalCategory category, CriteriaBuilder builder) {
        var typeLine = builder.lower(card.get("typeLine"));
        var oracleText = builder.lower(card.get("oracleText"));
        List<Predicate> higherPriority = new ArrayList<>();
        for (var candidate : PRIORITY_ORDER) {
            Predicate ownMarkers = markerPredicate(candidate, typeLine, oracleText, builder);
            if (candidate == category) {
                return builder.and(ownMarkers, builder.not(anyOf(higherPriority, builder)));
            }
            higherPriority.add(ownMarkers);
        }
        // SYNERGY: none of the higher-priority markers match.
        return builder.not(anyOf(higherPriority, builder));
    }

    private static Predicate markerPredicate(
            CardFunctionalCategory category,
            Expression<String> typeLine,
            Expression<String> oracleText,
            CriteriaBuilder builder) {
        return switch (category) {
            case LAND -> contains(typeLine, CardFunctionalCategory.LAND_MARKER, builder);
            case RAMP -> rampPredicate(oracleText, builder);
            case DRAW -> contains(oracleText, CardFunctionalCategory.DRAW_MARKER, builder);
            case WIPE -> wipePredicate(oracleText, builder);
            case REMOVAL -> removalPredicate(oracleText, builder);
            case SYNERGY -> builder.disjunction();
        };
    }

    private static Predicate rampPredicate(Expression<String> oracleText, CriteriaBuilder builder) {
        return builder.or(
                contains(oracleText, CardFunctionalCategory.RAMP_MANA_MARKER, builder),
                builder.and(
                        contains(oracleText, CardFunctionalCategory.SEARCH_LIBRARY_MARKER, builder),
                        contains(oracleText, CardFunctionalCategory.LAND_MARKER, builder)));
    }

    private static Predicate wipePredicate(Expression<String> oracleText, CriteriaBuilder builder) {
        return builder.or(
                contains(oracleText, CardFunctionalCategory.DESTROY_ALL_MARKER, builder),
                contains(oracleText, CardFunctionalCategory.EXILE_ALL_MARKER, builder));
    }

    private static Predicate removalPredicate(
            Expression<String> oracleText, CriteriaBuilder builder) {
        return builder.or(
                contains(oracleText, CardFunctionalCategory.DESTROY_TARGET_MARKER, builder),
                contains(oracleText, CardFunctionalCategory.EXILE_TARGET_MARKER, builder));
    }

    private static Predicate anyOf(List<Predicate> predicates, CriteriaBuilder builder) {
        return predicates.isEmpty()
                ? builder.disjunction()
                : builder.or(predicates.toArray(new Predicate[0]));
    }

    private static Predicate contains(
            Expression<String> text, String marker, CriteriaBuilder builder) {
        return builder.like(text, "%" + marker + "%");
    }
}
