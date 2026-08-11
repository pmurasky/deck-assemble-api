package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionCardNotFoundException;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationPlanner.AllocationSlice;
import com.deckassemble.collections.application.physical.PhysicalDeckLookup.DeckCardView;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocation;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocationRepository;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PhysicalCardAllocationService {

    private final PhysicalDeckLookup deckLookup;
    private final PhysicalCardInventory inventory;
    private final PhysicalCardAllocationRepository allocationRepository;
    private final PhysicalCardAvailabilityCalculator availabilityCalculator;
    private final PhysicalCardAllocationViews allocationViews;

    public PhysicalCardAllocationService(
            PhysicalDeckLookup deckLookup,
            PhysicalCardInventory inventory,
            PhysicalCardAllocationRepository allocationRepository,
            PhysicalCardAvailabilityCalculator availabilityCalculator,
            PhysicalCardAllocationViews allocationViews) {
        this.deckLookup = deckLookup;
        this.inventory = inventory;
        this.allocationRepository = allocationRepository;
        this.availabilityCalculator = availabilityCalculator;
        this.allocationViews = allocationViews;
    }

    public AllocationView allocate(long deckId, AllocationCommand command) {
        deckLookup.ownedLocked(deckId);
        DeckCardView deckCard =
                deckLookup.deckCard(deckId, requiredDeckCardId(command.deckCardId()));
        int quantity = requestedQuantity(command.quantity(), deckCard.quantity());
        List<CollectionCard> cards = lockedCards(deckCard);
        Map<Long, Integer> allocated = inventory.allocatedByCollectionCardId(cards, null);
        List<AllocationSlice> slices =
                PhysicalCardAllocationPlanner.plan(
                        cards, allocated, command.collectionCardId(), quantity);
        assertDeckCardCapacity(deckCard, quantity, null);
        return allocationViews.forAllocation(saveAllocations(deckId, deckCard, slices).getFirst());
    }

    @Transactional(readOnly = true)
    public List<AllocationView> list(long deckId) {
        deckLookup.owned(deckId);
        return allocationRepository.findByDeckIdOrderById(deckId).stream()
                .map(allocationViews::forAllocation)
                .toList();
    }

    public AllocationView update(long deckId, long allocationId, AllocationCommand command) {
        deckLookup.ownedLocked(deckId);
        PhysicalCardAllocation allocation = ownedAllocation(deckId, allocationId);
        DeckCardView deckCard = deckLookup.deckCard(deckId, allocation.getDeckCardId());
        int quantity = requiredQuantity(command.quantity());
        CollectionCard card = lockedAllocationCard(deckCard, allocation.getCollectionCardId());
        assertAvailable(card, quantity, allocationId);
        assertDeckCardCapacity(deckCard, quantity, allocationId);
        allocation.setQuantity(quantity);
        return allocationViews.forAllocation(allocationRepository.save(allocation));
    }

    public void release(long deckId, long allocationId) {
        deckLookup.ownedLocked(deckId);
        allocationRepository.delete(ownedAllocation(deckId, allocationId));
    }

    public void releaseDeck(long deckId) {
        deckLookup.ownedLocked(deckId);
        allocationRepository.deleteByDeckId(deckId);
    }

    @Transactional(readOnly = true)
    public List<AllocationView> unavailable(long deckId) {
        deckLookup.owned(deckId);
        return availabilityFor(inventory.profileId(), deckId, availabilityRequests(deckId)).stream()
                .filter(availability -> availability.missingQuantity() > 0)
                .map(availability -> allocationViews.unavailable(deckId, availability))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardAvailability> availabilityFor(
            long profileId, long deckId, List<DeckCardAvailabilityRequest> deckCards) {
        return availabilityCalculator.availabilityFor(profileId, deckId, deckCards);
    }

    private List<PhysicalCardAllocation> saveAllocations(
            long deckId, DeckCardView deckCard, List<AllocationSlice> slices) {
        return slices.stream()
                .map(slice -> saveAllocation(deckId, deckCard, slice.card(), slice.quantity()))
                .toList();
    }

    private PhysicalCardAllocation saveAllocation(
            long deckId, DeckCardView deckCard, CollectionCard card, int quantity) {
        return allocationRepository
                .findByDeckCardIdAndCollectionCardId(deckCard.id(), card.getId())
                .map(existing -> increase(existing, quantity))
                .orElseGet(
                        () ->
                                allocationRepository.save(
                                        new PhysicalCardAllocation(
                                                deckId, deckCard.id(), card.getId(), quantity)));
    }

    private PhysicalCardAllocation increase(PhysicalCardAllocation allocation, int quantity) {
        allocation.setQuantity(allocation.getQuantity() + quantity);
        return allocationRepository.save(allocation);
    }

    private void assertAvailable(CollectionCard card, int quantity, @Nullable Long excludedId) {
        int allocated =
                inventory
                        .allocatedByCollectionCardId(List.of(card), excludedId)
                        .getOrDefault(card.getId(), 0);
        if (inventory.ownedQuantity(card) - allocated < quantity) {
            throw notEnoughCopies();
        }
    }

    private void assertDeckCardCapacity(
            DeckCardView card, int addedQuantity, @Nullable Long excludedAllocationId) {
        int allocated =
                allocationRepository.sumByDeckCardIdExcluding(card.id(), excludedAllocationId);
        if (allocated + addedQuantity > card.quantity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Deck card is already fully allocated.");
        }
    }

    private CollectionCard lockedAllocationCard(DeckCardView deckCard, long collectionCardId) {
        return lockedCards(deckCard).stream()
                .filter(card -> card.getId().equals(collectionCardId))
                .findFirst()
                .orElseThrow(CollectionCardNotFoundException::new);
    }

    private List<CollectionCard> lockedCards(DeckCardView deckCard) {
        return inventory.lockedCards(deckCard);
    }

    private List<DeckCardAvailabilityRequest> availabilityRequests(long deckId) {
        return deckLookup.deckCards(deckId).stream()
                .map(
                        card ->
                                new DeckCardAvailabilityRequest(
                                        card.id(), card.cardPrintingId(), card.quantity()))
                .toList();
    }

    private int requestedQuantity(@Nullable Integer requested, int defaultQuantity) {
        return requested == null ? defaultQuantity : requiredQuantity(requested);
    }

    private int requiredQuantity(@Nullable Integer quantity) {
        if (quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is required.");
        }
        return quantity;
    }

    private long requiredDeckCardId(@Nullable Long deckCardId) {
        if (deckCardId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deck card id is required.");
        }
        return deckCardId;
    }

    private PhysicalCardAllocation ownedAllocation(long deckId, long allocationId) {
        return allocationRepository
                .findByIdAndDeckId(allocationId, deckId)
                .orElseThrow(CollectionCardNotFoundException::new);
    }

    static ResponseStatusException notEnoughCopies() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "Not enough physical copies available.");
    }

    public record AllocationCommand(
            @Nullable Long deckCardId,
            @Nullable Long collectionCardId,
            @Nullable Integer quantity) {}

    public record DeckCardAvailabilityRequest(long deckCardId, long cardPrintingId, int quantity) {}

    public record CardAvailability(
            long deckCardId,
            long cardPrintingId,
            int deckQuantity,
            int ownedQuantity,
            int allocatedQuantity,
            int availableQuantity,
            int missingQuantity) {}

    public record AllocationView(
            @Nullable Long id,
            long deckId,
            long deckCardId,
            long deckCardPrintingId,
            @Nullable Long collectionCardId,
            @Nullable Long collectionCardPrintingId,
            int deckQuantity,
            int quantity,
            int ownedQuantity,
            int allocatedQuantity,
            int availableQuantity,
            int missingQuantity,
            boolean exactPrinting) {}
}
