package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionCardNotFoundException;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationPartView;
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

    public AllocationView forAllocations(List<PhysicalCardAllocation> allocations) {
        if (allocations.size() == 1) {
            return forAllocation(allocations.getFirst());
        }
        List<AllocationView> views = allocations.stream().map(this::forAllocation).toList();
        return aggregateView(views);
    }

    private AllocationView aggregateView(List<AllocationView> views) {
        AllocationView first = views.getFirst();
        int quantity = sum(views, AllocationView::quantity);
        return new AllocationView(
                first.id(),
                first.deckId(),
                first.deckCardId(),
                first.deckCardPrintingId(),
                null,
                null,
                first.deckQuantity(),
                quantity,
                sum(views, AllocationView::ownedQuantity),
                sum(views, AllocationView::allocatedQuantity),
                sum(views, AllocationView::availableQuantity),
                Math.max(0, first.deckQuantity() - quantity),
                views.stream().allMatch(AllocationView::exactPrinting),
                views.stream().flatMap(view -> view.allocations().stream()).toList());
    }

    private int sum(
            List<AllocationView> views, java.util.function.ToIntFunction<AllocationView> mapper) {
        return views.stream().mapToInt(mapper).sum();
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
                false,
                List.of());
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
                deckCard.cardPrintingId() == card.getCardPrintingId(),
                List.of(part(allocation, card, deckCard)));
    }

    private AllocationPartView part(
            PhysicalCardAllocation allocation, CollectionCard card, DeckCardView deckCard) {
        return new AllocationPartView(
                allocation.getId(),
                allocation.getCollectionCardId(),
                card.getCardPrintingId(),
                allocation.getQuantity(),
                deckCard.cardPrintingId() == card.getCardPrintingId());
    }

    private CollectionCard collectionCard(long collectionCardId) {
        return collectionCardRepository
                .findById(collectionCardId)
                .orElseThrow(CollectionCardNotFoundException::new);
    }
}
