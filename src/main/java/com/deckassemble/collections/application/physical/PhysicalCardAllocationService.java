package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.application.CollectionCardNotFoundException;
import com.deckassemble.collections.application.physical.PhysicalDeckLookup.DeckCardView;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocation;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocationRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PhysicalCardAllocationService {

    private final PhysicalDeckLookup deckLookup;
    private final CollectionAccessGuard collectionAccessGuard;
    private final CollectionCardRepository collectionCardRepository;
    private final PhysicalCardAllocationRepository allocationRepository;

    public PhysicalCardAllocationService(
            PhysicalDeckLookup deckLookup,
            CollectionAccessGuard collectionAccessGuard,
            CollectionCardRepository collectionCardRepository,
            PhysicalCardAllocationRepository allocationRepository) {
        this.deckLookup = deckLookup;
        this.collectionAccessGuard = collectionAccessGuard;
        this.collectionCardRepository = collectionCardRepository;
        this.allocationRepository = allocationRepository;
    }

    public AllocationView allocate(long deckId, AllocationCommand command) {
        deckLookup.ownedLocked(deckId);
        DeckCardView deckCard =
                deckLookup.deckCard(deckId, requiredDeckCardId(command.deckCardId()));
        int quantity = requestedQuantity(command.quantity(), deckCard.quantity());
        List<CollectionCard> cards = lockedCards(deckCard);
        Map<Long, Integer> allocated = allocatedByCollectionCardId(cards, null);
        CollectionCard selected =
                selectedCard(cards, allocated, command.collectionCardId(), quantity);
        assertDeckCardCapacity(deckCard, quantity, null);
        PhysicalCardAllocation saved = saveAllocation(deckId, deckCard, selected, quantity);
        return viewFor(saved);
    }

    @Transactional(readOnly = true)
    public List<AllocationView> list(long deckId) {
        deckLookup.owned(deckId);
        return allocationRepository.findByDeckIdOrderById(deckId).stream()
                .map(this::viewFor)
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
        return viewFor(allocationRepository.save(allocation));
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
        return availabilityFor(
                        collectionAccessGuard.profileId(), deckId, availabilityRequests(deckId))
                .stream()
                .filter(availability -> availability.missingQuantity() > 0)
                .map(availability -> unavailableView(deckId, availability))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CardAvailability> availabilityFor(
            long profileId, long deckId, List<DeckCardAvailabilityRequest> deckCards) {
        return deckCards.stream()
                .map(card -> availability(profileId, deckId, card))
                .sorted(Comparator.comparing(CardAvailability::deckCardId))
                .toList();
    }

    private CardAvailability availability(
            long profileId, long deckId, DeckCardAvailabilityRequest card) {
        List<CollectionCard> cards = compatibleCards(profileId, card.cardPrintingId());
        int owned = cards.stream().mapToInt(this::ownedQuantity).sum();
        int allocated =
                allocatedByCollectionCardId(cards, null).values().stream().mapToInt(i -> i).sum();
        int current = currentDeckCardAllocated(deckId, card.deckCardId());
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

    private AllocationView unavailableView(long deckId, CardAvailability availability) {
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

    private CollectionCard selectedCard(
            List<CollectionCard> cards,
            Map<Long, Integer> allocated,
            @Nullable Long requestedCollectionCardId,
            int quantity) {
        return cards.stream()
                .filter(
                        card ->
                                requestedCollectionCardId == null
                                        || card.getId().equals(requestedCollectionCardId))
                .filter(
                        card ->
                                ownedQuantity(card) - allocated.getOrDefault(card.getId(), 0)
                                        >= quantity)
                .findFirst()
                .orElseThrow(PhysicalCardAllocationService::notEnoughCopies);
    }

    private void assertAvailable(CollectionCard card, int quantity, @Nullable Long excludedId) {
        int allocated =
                allocatedByCollectionCardId(List.of(card), excludedId)
                        .getOrDefault(card.getId(), 0);
        if (ownedQuantity(card) - allocated < quantity) {
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

    private AllocationView viewFor(PhysicalCardAllocation allocation) {
        DeckCardView deckCard =
                deckLookup.deckCard(allocation.getDeckId(), allocation.getDeckCardId());
        CollectionCard card = collectionCard(allocation.getCollectionCardId());
        int allocated =
                allocatedByCollectionCardId(List.of(card), null).getOrDefault(card.getId(), 0);
        int owned = ownedQuantity(card);
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

    private CollectionCard lockedAllocationCard(DeckCardView deckCard, long collectionCardId) {
        return lockedCards(deckCard).stream()
                .filter(card -> card.getId().equals(collectionCardId))
                .findFirst()
                .orElseThrow(CollectionCardNotFoundException::new);
    }

    private List<CollectionCard> lockedCards(DeckCardView deckCard) {
        return collectionCardRepository.findCompatibleOwnedCardsLocked(
                collectionAccessGuard.profileId(), deckCard.cardPrintingId());
    }

    private List<CollectionCard> compatibleCards(long profileId, long cardPrintingId) {
        return collectionCardRepository.findCompatibleOwnedCards(profileId, cardPrintingId);
    }

    private Map<Long, Integer> allocatedByCollectionCardId(
            Collection<CollectionCard> cards, @Nullable Long excludedAllocationId) {
        List<Long> ids = cards.stream().map(CollectionCard::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return allocationRepository
                .sumByCollectionCardIdsExcluding(ids, excludedAllocationId)
                .stream()
                .collect(
                        Collectors.toMap(
                                PhysicalCardAllocationRepository.CollectionCardAllocationTotal
                                        ::getCollectionCardId,
                                PhysicalCardAllocationRepository.CollectionCardAllocationTotal
                                        ::getQuantity));
    }

    private int currentDeckCardAllocated(long deckId, long deckCardId) {
        return allocationRepository.findByDeckIdOrderById(deckId).stream()
                .filter(allocation -> allocation.getDeckCardId().equals(deckCardId))
                .mapToInt(PhysicalCardAllocation::getQuantity)
                .sum();
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

    private CollectionCard collectionCard(long collectionCardId) {
        return collectionCardRepository
                .findById(collectionCardId)
                .orElseThrow(CollectionCardNotFoundException::new);
    }

    private int ownedQuantity(CollectionCard card) {
        return card.getRegularQuantity() + card.getFoilQuantity();
    }

    private static ResponseStatusException notEnoughCopies() {
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
