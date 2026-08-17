package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.shared.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Builds the owned-quantity and price-range {@code cards} search specifications, both resolved from
 * a bounded, Java-computed candidate card-id set rather than a pure Criteria predicate (see each
 * method's {@code ponytail:} note). Split out of {@link CardCatalogService} because these two
 * filters are the only ones needing cross-cutting lookups (the owned-quantity port, the price
 * service) rather than being pure {@link CardSearchPredicates} predicates.
 */
@Component
class CardSearchCandidateSpecifications {

    private final CardPrintingRepository cardPrintingRepository;
    private final CurrentUser currentUser;
    private final CardOwnershipLookup cardOwnershipLookup;
    private final CardPriceService cardPriceService;

    CardSearchCandidateSpecifications(
            CardPrintingRepository cardPrintingRepository,
            CurrentUser currentUser,
            CardOwnershipLookup cardOwnershipLookup,
            CardPriceService cardPriceService) {
        this.cardPrintingRepository = cardPrintingRepository;
        this.currentUser = currentUser;
        this.cardOwnershipLookup = cardOwnershipLookup;
        this.cardPriceService = cardPriceService;
    }

    Specification<Card> ownedQuantitySpec(CardSearchFilter.IntRange range) {
        int min = range.min() == null ? 0 : range.min();
        int max = range.max() == null ? Integer.MAX_VALUE : range.max();
        Map<Long, Integer> quantityByCard = ownedQuantitiesByCard();
        Set<Long> matchingIds =
                quantityByCard.entrySet().stream()
                        .filter(entry -> entry.getValue() >= min && entry.getValue() <= max)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
        return candidateIdSpec(matchingIds, min <= 0, quantityByCard.keySet());
    }

    /**
     * Owned total quantity (regular + foil) per card id for the current user, or {@code null} when
     * there is no authenticated profile, so search responses can omit ownership without erroring.
     */
    @Nullable Map<Long, Integer> ownedQuantitiesByCardOrNull() {
        return currentUser.subject().isPresent() ? ownedQuantitiesByCard() : null;
    }

    // Justified: method-local map, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<Long, Integer> ownedQuantitiesByCard() {
        Map<Long, Integer> quantityByPrinting =
                currentUser
                        .subject()
                        .map(cardOwnershipLookup::ownedQuantitiesBySubject)
                        .orElse(Map.of());
        if (quantityByPrinting.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> cardIdByPrinting = cardIdsByPrintingId(quantityByPrinting.keySet());
        Map<Long, Integer> quantityByCard = new HashMap<>();
        quantityByPrinting.forEach(
                (printingId, quantity) -> {
                    Long cardId = cardIdByPrinting.get(printingId);
                    if (cardId != null) {
                        quantityByCard.merge(cardId, quantity, Integer::sum);
                    }
                });
        return quantityByCard;
    }

    // ponytail: candidate ids are computed in Java from the caller's bounded owned collection,
    // same shape as CardCatalogService's existing partner-candidate pattern; push to a SQL
    // subquery if collection sizes make this slow.
    private static Specification<Card> candidateIdSpec(
            Set<Long> matchingIds, boolean includeUnmatched, Set<Long> excludedFromUnmatched) {
        return (root, criteriaQuery, builder) -> {
            Predicate matches =
                    matchingIds.isEmpty() ? builder.disjunction() : root.get("id").in(matchingIds);
            if (!includeUnmatched) {
                return matches;
            }
            Predicate outsideOwned =
                    excludedFromUnmatched.isEmpty()
                            ? builder.conjunction()
                            : root.get("id").in(excludedFromUnmatched).not();
            return builder.or(matches, outsideOwned);
        };
    }

    // ponytail: materializes every tracked price to filter in Java, reusing CardPriceService's
    // existing latest-snapshot lookup rather than duplicating a correlated-subquery; push to SQL
    // if the tracked-price table size makes this slow.
    Specification<Card> priceRangeSpec(CardSearchFilter.PriceRange range) {
        Set<Long> trackedPrintingIds = cardPriceService.trackedPrintingIds();
        Map<Long, CardPrice> prices = cardPriceService.latestPrices(trackedPrintingIds);
        Map<Long, Long> cardIdByPrinting = cardIdsByPrintingId(trackedPrintingIds);
        Set<Long> matchingIds =
                prices.entrySet().stream()
                        .filter(entry -> inRange(entry.getValue(), range))
                        .map(entry -> cardIdByPrinting.get(entry.getKey()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        return matchingIds.isEmpty()
                ? (root, criteriaQuery, builder) -> builder.disjunction()
                : (root, criteriaQuery, builder) -> root.get("id").in(matchingIds);
    }

    private static boolean inRange(CardPrice price, CardSearchFilter.PriceRange range) {
        BigDecimal value = price.forCurrency(range.currency());
        if (value == null) {
            return false;
        }
        if (range.min() != null && value.compareTo(range.min()) < 0) {
            return false;
        }
        return range.max() == null || value.compareTo(range.max()) <= 0;
    }

    private Map<Long, Long> cardIdsByPrintingId(Collection<Long> printingIds) {
        return cardPrintingRepository.findAllById(printingIds).stream()
                .collect(
                        Collectors.toMap(
                                CardPrinting::getId, printing -> printing.getCard().getId()));
    }
}
