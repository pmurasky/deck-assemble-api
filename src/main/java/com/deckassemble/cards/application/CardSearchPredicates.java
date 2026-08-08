package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrinting;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

// Justified: class-total complexity is the sum of ~20 small, single-purpose predicate-building
// methods (each well under the per-method limit, highest is 6); one typed filter per method is
// the design, not a smell.
@SuppressWarnings("PMD.CyclomaticComplexity")
final class CardSearchPredicates {

    // ponytail: power/toughness are text columns (MTG allows "*", "X", "1+*"); the range filter
    // matches literal digit strings, so non-numeric values simply never match a numeric range.
    // Bounded at the request layer (@Min/@Max) to keep the generated IN list small.
    static final int MAX_POWER_TOUGHNESS = 20;

    private CardSearchPredicates() {}

    static Predicate cardPredicate(
            From<?, Card> card,
            CardSearchFilter filter,
            CriteriaQuery<?> criteria,
            CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.isTrue(card.get("active")));
        predicates.add(nameOrFlavorNamePredicate(card, filter.query(), criteria, builder));
        addTextFilters(predicates, card, filter, builder);
        addRangeFilters(predicates, card, filter, builder);
        addRelatedEntityFilters(predicates, card, filter, criteria, builder);
        if (Boolean.TRUE.equals(filter.commanderEligible())) {
            predicates.add(commanderEligiblePredicate(card, builder));
        }
        if (Boolean.TRUE.equals(filter.gameChanger())) {
            predicates.add(builder.isTrue(card.get("gameChanger")));
        }
        return builder.and(predicates.toArray(new Predicate[0]));
    }

    private static void addTextFilters(
            List<Predicate> predicates,
            From<?, Card> card,
            CardSearchFilter filter,
            CriteriaBuilder builder) {
        if (filter.colorIdentity() != null) {
            predicates.add(
                    builder.like(card.get("colorIdentity"), "%" + filter.colorIdentity() + "%"));
        }
        if (filter.type() != null) {
            predicates.add(containsIgnoreCase(card.get("typeLine"), filter.type(), builder));
        }
        if (filter.oracleText() != null) {
            predicates.add(
                    containsIgnoreCase(card.get("oracleText"), filter.oracleText(), builder));
        }
        if (filter.keyword() != null) {
            predicates.add(containsIgnoreCase(card.get("keywords"), filter.keyword(), builder));
        }
    }

    private static void addRangeFilters(
            List<Predicate> predicates,
            From<?, Card> card,
            CardSearchFilter filter,
            CriteriaBuilder builder) {
        if (filter.manaValueRange() != null) {
            addBounds(predicates, card.get("manaValue"), filter.manaValueRange(), builder);
        }
        if (filter.powerRange() != null) {
            predicates.add(numericTextRangePredicate(card, "power", filter.powerRange(), builder));
        }
        if (filter.toughnessRange() != null) {
            predicates.add(
                    numericTextRangePredicate(card, "toughness", filter.toughnessRange(), builder));
        }
    }

    private static void addBounds(
            List<Predicate> predicates,
            Expression<BigDecimal> value,
            CardSearchFilter.BigDecimalRange range,
            CriteriaBuilder builder) {
        if (range.min() != null) {
            predicates.add(builder.ge(value, range.min()));
        }
        if (range.max() != null) {
            predicates.add(builder.le(value, range.max()));
        }
    }

    private static Predicate numericTextRangePredicate(
            From<?, Card> card,
            String attribute,
            CardSearchFilter.IntRange range,
            CriteriaBuilder builder) {
        int min = range.min() == null ? 0 : Math.max(range.min(), 0);
        int max = range.max() == null ? MAX_POWER_TOUGHNESS : range.max();
        if (min > max) {
            return builder.disjunction();
        }
        List<String> values = IntStream.rangeClosed(min, max).mapToObj(String::valueOf).toList();
        return card.get(attribute).in(values);
    }

    private static void addRelatedEntityFilters(
            List<Predicate> predicates,
            From<?, Card> card,
            CardSearchFilter filter,
            CriteriaQuery<?> criteria,
            CriteriaBuilder builder) {
        if (filter.formatLegality() != null) {
            predicates.add(
                    formatLegalityPredicate(card, filter.formatLegality(), criteria, builder));
        }
        if (filter.functionalCategory() != null) {
            predicates.add(
                    CardFunctionalCategoryPredicates.categoryPredicate(
                            card, filter.functionalCategory(), builder));
        }
        if (filter.printingFilter() != null && !filter.printingFilter().isEmpty()) {
            predicates.add(
                    printingFilterPredicate(card, filter.printingFilter(), criteria, builder));
        }
    }

    private static Predicate formatLegalityPredicate(
            From<?, Card> card,
            CardSearchFilter.FormatLegality legality,
            CriteriaQuery<?> criteria,
            CriteriaBuilder builder) {
        var subquery = criteria.subquery(Long.class);
        var legalities = subquery.from(CardLegality.class);
        String status = legality.legalityStatus() == null ? "legal" : legality.legalityStatus();
        return builder.exists(
                subquery.select(legalities.get("id"))
                        .where(
                                builder.equal(legalities.get("card").get("id"), card.get("id")),
                                builder.equal(legalities.get("formatCode"), legality.formatCode()),
                                builder.equal(legalities.get("legalityStatus"), status)));
    }

    private static Predicate printingFilterPredicate(
            From<?, Card> card,
            CardSearchFilter.PrintingFilter filter,
            CriteriaQuery<?> criteria,
            CriteriaBuilder builder) {
        var subquery = criteria.subquery(Long.class);
        var printings = subquery.from(CardPrinting.class);
        List<Predicate> conditions = new ArrayList<>();
        conditions.add(builder.equal(printings.get("card").get("id"), card.get("id")));
        addPrintingConditions(conditions, printings, filter, builder);
        return builder.exists(
                subquery.select(printings.get("id")).where(conditions.toArray(new Predicate[0])));
    }

    private static void addPrintingConditions(
            List<Predicate> conditions,
            From<?, CardPrinting> printings,
            CardSearchFilter.PrintingFilter filter,
            CriteriaBuilder builder) {
        if (filter.setCode() != null) {
            conditions.add(
                    builder.equal(printings.get("magicSet").get("setCode"), filter.setCode()));
        }
        if (filter.rarity() != null) {
            conditions.add(equalsIgnoreCase(printings.get("rarity"), filter.rarity(), builder));
        }
        if (filter.collectorNumber() != null) {
            conditions.add(
                    builder.equal(printings.get("collectorNumber"), filter.collectorNumber()));
        }
        if (filter.language() != null) {
            conditions.add(equalsIgnoreCase(printings.get("language"), filter.language(), builder));
        }
        if (filter.finish() != null) {
            conditions.add(finishPredicate(printings, filter.finish(), builder));
        }
    }

    private static Predicate finishPredicate(
            From<?, CardPrinting> printings, String finish, CriteriaBuilder builder) {
        return "foil".equalsIgnoreCase(finish)
                ? builder.isTrue(printings.get("foilAvailable"))
                : builder.isTrue(printings.get("nonfoilAvailable"));
    }

    private static Predicate nameOrFlavorNamePredicate(
            From<?, Card> card, String query, CriteriaQuery<?> criteria, CriteriaBuilder builder) {
        var queryLike = "%" + query.toLowerCase() + "%";
        return builder.or(
                builder.like(builder.lower(card.get("name")), queryLike),
                flavorNameExists(card, criteria, builder, queryLike));
    }

    private static Predicate flavorNameExists(
            From<?, Card> card,
            CriteriaQuery<?> criteria,
            CriteriaBuilder builder,
            String queryLike) {
        var subquery = criteria.subquery(Long.class);
        var printings = subquery.from(CardPrinting.class);
        return builder.exists(
                subquery.select(printings.get("id"))
                        .where(
                                builder.equal(printings.get("card").get("id"), card.get("id")),
                                builder.like(
                                        builder.lower(printings.get("flavorName")), queryLike)));
    }

    private static Predicate commanderEligiblePredicate(
            From<?, Card> card, CriteriaBuilder builder) {
        var typeLine = builder.lower(card.get("typeLine"));
        var legendaryCreature =
                builder.and(
                        builder.like(typeLine, "%legendary%"),
                        builder.like(typeLine, "%creature%"));
        var canBeCommander =
                builder.like(builder.lower(card.get("oracleText")), "%can be your commander%");
        return builder.or(legendaryCreature, canBeCommander);
    }

    private static Predicate containsIgnoreCase(
            Expression<String> text, String value, CriteriaBuilder builder) {
        return builder.like(builder.lower(text), "%" + value.toLowerCase() + "%");
    }

    private static Predicate equalsIgnoreCase(
            Expression<String> text, String value, CriteriaBuilder builder) {
        return builder.equal(builder.lower(text), value.toLowerCase());
    }
}
