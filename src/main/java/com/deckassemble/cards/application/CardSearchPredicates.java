package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

final class CardSearchPredicates {

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
        if (filter.setCode() != null) {
            predicates.add(setCodePredicate(card, filter.setCode(), criteria, builder));
        }
        if (Boolean.TRUE.equals(filter.commanderEligible())) {
            predicates.add(commanderEligiblePredicate(card, builder));
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
            predicates.add(
                    builder.like(
                            builder.lower(card.get("typeLine")),
                            "%" + filter.type().toLowerCase() + "%"));
        }
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

    private static Predicate setCodePredicate(
            From<?, Card> card,
            String setCode,
            CriteriaQuery<?> criteria,
            CriteriaBuilder builder) {
        var subquery = criteria.subquery(Long.class);
        var printings = subquery.from(CardPrinting.class);
        return builder.exists(
                subquery.select(printings.get("id"))
                        .where(
                                builder.equal(printings.get("card").get("id"), card.get("id")),
                                builder.equal(printings.get("magicSet").get("setCode"), setCode)));
    }
}
