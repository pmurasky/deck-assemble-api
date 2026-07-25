package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.collections.application.CollectionService;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves whether cards are owned by matching oracle ids: owning any printing of a card counts as
 * owning the card (spec decision D3).
 */
@Component
public class OwnershipChecker {

    private final CardCatalogService cardCatalogService;
    private final CollectionService collectionService;

    public OwnershipChecker(
            CardCatalogService cardCatalogService, CollectionService collectionService) {
        this.cardCatalogService = cardCatalogService;
        this.collectionService = collectionService;
    }

    public boolean isOwned(long profileId, long cardPrintingId) {
        return !filterOwnedPrintingIds(profileId, List.of(cardPrintingId)).isEmpty();
    }

    public Set<Long> filterOwnedPrintingIds(long profileId, Collection<Long> cardPrintingIds) {
        if (cardPrintingIds.isEmpty()) {
            return Set.of();
        }
        Map<Long, String> oracleByPrinting =
                cardCatalogService.getOracleIdsByPrintingIds(cardPrintingIds);
        Set<Long> ownedPrintingIds = collectionService.getOwnedPrintingIds(profileId);
        if (ownedPrintingIds.isEmpty()) {
            return Set.of();
        }
        Set<String> ownedOracleIds =
                new HashSet<>(
                        cardCatalogService.getOracleIdsByPrintingIds(ownedPrintingIds).values());
        return cardPrintingIds.stream()
                .filter(id -> ownedOracleIds.contains(oracleByPrinting.get(id)))
                .collect(Collectors.toSet());
    }
}
