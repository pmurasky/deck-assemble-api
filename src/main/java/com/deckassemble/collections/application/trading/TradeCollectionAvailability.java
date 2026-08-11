package com.deckassemble.collections.application.trading;

import com.deckassemble.collections.application.physical.PhysicalCardInventory;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.trading.TradeListItem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class TradeCollectionAvailability {

    private final PhysicalCardInventory inventory;
    private final TradePhysicalMetadataMatcher metadataMatcher;

    TradeCollectionAvailability(
            PhysicalCardInventory inventory, TradePhysicalMetadataMatcher metadataMatcher) {
        this.inventory = inventory;
        this.metadataMatcher = metadataMatcher;
    }

    List<CollectionCard> compatibleCards(long profileId, long cardPrintingId) {
        return inventory.compatibleCards(profileId, cardPrintingId);
    }

    int remaining(CollectionCard card, Map<Long, Integer> remainingByCard) {
        return remainingByCard.computeIfAbsent(card.getId(), id -> available(card));
    }

    boolean metadataMatches(CollectionCard card, TradeListItem offered, TradeListItem wanted) {
        return metadataMatcher.matches(card.getId(), offered)
                && metadataMatcher.matches(card.getId(), wanted);
    }

    private int available(CollectionCard card) {
        return inventory.ownedQuantity(card)
                - inventory
                        .allocatedByCollectionCardId(List.of(card), null)
                        .getOrDefault(card.getId(), 0);
    }
}
