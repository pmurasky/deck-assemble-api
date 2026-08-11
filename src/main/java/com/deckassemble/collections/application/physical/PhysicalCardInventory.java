package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocation;
import com.deckassemble.collections.domain.physical.PhysicalCardAllocationRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PhysicalCardInventory {

    private final CollectionAccessGuard collectionAccessGuard;
    private final CollectionCardRepository collectionCardRepository;
    private final PhysicalCardAllocationRepository allocationRepository;

    public PhysicalCardInventory(
            CollectionAccessGuard collectionAccessGuard,
            CollectionCardRepository collectionCardRepository,
            PhysicalCardAllocationRepository allocationRepository) {
        this.collectionAccessGuard = collectionAccessGuard;
        this.collectionCardRepository = collectionCardRepository;
        this.allocationRepository = allocationRepository;
    }

    public long profileId() {
        return collectionAccessGuard.profileId();
    }

    public List<CollectionCard> lockedCards(PhysicalDeckLookup.DeckCardView deckCard) {
        return collectionCardRepository.findCompatibleOwnedCardsLocked(
                profileId(), deckCard.cardPrintingId());
    }

    public List<CollectionCard> compatibleCards(long profileId, long cardPrintingId) {
        return collectionCardRepository.findCompatibleOwnedCards(profileId, cardPrintingId);
    }

    public Map<Long, Integer> allocatedByCollectionCardId(
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

    public int currentDeckCardAllocated(long deckId, long deckCardId) {
        return allocationRepository.findByDeckIdOrderById(deckId).stream()
                .filter(allocation -> allocation.getDeckCardId().equals(deckCardId))
                .mapToInt(PhysicalCardAllocation::getQuantity)
                .sum();
    }

    public int ownedQuantity(CollectionCard card) {
        return card.getRegularQuantity() + card.getFoilQuantity();
    }
}
