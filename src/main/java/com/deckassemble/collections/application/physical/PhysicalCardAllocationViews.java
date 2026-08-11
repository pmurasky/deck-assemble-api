package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionCardNotFoundException;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationView;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.CardAvailability;
import com.deckassemble.collections.application.physical.PhysicalDeckLookup.DeckCardView;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocation;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PhysicalCardAllocationViews {

    private final PhysicalDeckLookup deckLookup;
    private final CollectionCardRepository collectionCardRepository;
    private final PhysicalCardInventory inventory;

    public PhysicalCardAllocationViews(
            PhysicalDeckLookup deckLookup,
            CollectionCardRepository collectionCardRepository,
            PhysicalCardInventory inventory) {
        this.deckLookup = deckLookup;
        this.collectionCardRepository = collectionCardRepository;
        this.inventory = inventory;
    }

    public AllocationView forAllocation(PhysicalCardAllocation allocation) {
        DeckCardView deckCard =
                deckLookup.deckCard(allocation.getDeckId(), allocation.getDeckCardId());
        CollectionCard card = collectionCard(allocation.getCollectionCardId());
        int allocated =
                inventory
                        .allocatedByCollectionCardId(List.of(card), null)
                        .getOrDefault(card.getId(), 0);
        return allocationView(allocation, deckCard, card, allocated);
    }

    public AllocationView unavailable(long deckId, CardAvailability availability) {
        return new AllocationView(
                null,
                deckId,
                availability.deckCardId(),
                availability.cardPrintingId(),
                null,
                null,
                availability.deckQuantity(),
                0,
                availability.ownedQuantity(),
                availability.allocatedQuantity(),
                availability.availableQuantity(),
                availability.missingQuantity(),
                false);
    }

    private AllocationView allocationView(
            PhysicalCardAllocation allocation,
            DeckCardView deckCard,
            CollectionCard card,
            int allocated) {
        int owned = inventory.ownedQuantity(card);
        return new AllocationView(
                allocation.getId(),
                allocation.getDeckId(),
                allocation.getDeckCardId(),
                deckCard.cardPrintingId(),
                allocation.getCollectionCardId(),
                card.getCardPrintingId(),
                deckCard.quantity(),
                allocation.getQuantity(),
                owned,
                allocated,
                owned - allocated,
                Math.max(0, deckCard.quantity() - allocation.getQuantity()),
                deckCard.cardPrintingId() == card.getCardPrintingId());
    }

    private CollectionCard collectionCard(long collectionCardId) {
        return collectionCardRepository
                .findById(collectionCardId)
                .orElseThrow(CollectionCardNotFoundException::new);
    }
}
