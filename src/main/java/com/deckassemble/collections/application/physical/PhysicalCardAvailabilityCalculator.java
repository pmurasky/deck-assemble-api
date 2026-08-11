package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.CardAvailability;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.DeckCardAvailabilityRequest;
import com.deckassemble.collections.domain.CollectionCard;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PhysicalCardAvailabilityCalculator {

    private final PhysicalCardInventory inventory;

    public PhysicalCardAvailabilityCalculator(PhysicalCardInventory inventory) {
        this.inventory = inventory;
    }

    public List<CardAvailability> availabilityFor(
            long profileId, long deckId, List<DeckCardAvailabilityRequest> deckCards) {
        return deckCards.stream()
                .map(card -> availability(profileId, deckId, card))
                .sorted(Comparator.comparing(CardAvailability::deckCardId))
                .toList();
    }

    private CardAvailability availability(
            long profileId, long deckId, DeckCardAvailabilityRequest card) {
        List<CollectionCard> cards = inventory.compatibleCards(profileId, card.cardPrintingId());
        int owned = cards.stream().mapToInt(inventory::ownedQuantity).sum();
        int allocated =
                inventory.allocatedByCollectionCardId(cards, null).values().stream()
                        .mapToInt(i -> i)
                        .sum();
        int current = inventory.currentDeckCardAllocated(deckId, card.deckCardId());
        int available = Math.max(0, owned - allocated + current);
        return new CardAvailability(
                card.deckCardId(),
                card.cardPrintingId(),
                card.quantity(),
                owned,
                current,
                available,
                Math.max(0, card.quantity() - available));
    }
}
