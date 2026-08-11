package com.deckassemble.decks.application;

import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Syncs deck card ownership with the user's collection and acquires wishlist cards. */
@Service
@Transactional
public class DeckOwnershipService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;
    private final DeckCardService deckCardService;
    private final CollectionService collectionService;
    private final PhysicalCardAllocationService allocationService;

    public DeckOwnershipService(
            DeckAccessGuard deckAccessGuard,
            DeckCardRepository deckCardRepository,
            DeckCardService deckCardService,
            CollectionService collectionService,
            PhysicalCardAllocationService allocationService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
        this.deckCardService = deckCardService;
        this.collectionService = collectionService;
        this.allocationService = allocationService;
    }

    public OwnershipSyncResponse syncOwnership(long deckId) {
        deckAccessGuard.owned(deckId);
        long profileId = deckAccessGuard.profileId();
        List<DeckCard> cards = deckCardRepository.findByDeckId(deckId);
        var availability =
                allocationService.availabilityFor(profileId, deckId, availabilityRequests(cards));
        var availabilityByDeckCardId = availabilityByDeckCardId(availability);
        var changes =
                cards.stream()
                        .map(card -> syncCard(card, availabilityByDeckCardId))
                        .flatMap(Optional::stream)
                        .toList();
        return new OwnershipSyncResponse(
                changes.size(),
                changes,
                unavailableCount(availability),
                physicalAvailability(availability));
    }

    public DeckCardResponse acquireCard(long deckId, long deckCardId) {
        deckAccessGuard.owned(deckId);
        DeckCard deckCard = deckCardService.ownedCard(deckId, deckCardId);
        collectionService.addToDefaultCollection(
                deckCard.getCardPrintingId(), deckCard.getQuantity(), 0);
        if (deckCard.getOwnershipStatus() != DeckCard.OwnershipStatus.OWNED) {
            deckCard.setOwnershipStatus(DeckCard.OwnershipStatus.OWNED);
            deckCardRepository.save(deckCard);
        }
        return deckCardService.responseFor(deckCard);
    }

    private Optional<OwnershipSyncResponse.OwnershipChange> syncCard(
            DeckCard card,
            Map<Long, PhysicalCardAllocationService.CardAvailability> availabilityByDeckCardId) {
        var current = card.getOwnershipStatus();
        var availability = availabilityByDeckCardId.get(card.getId());
        int availableQuantity = availability == null ? 0 : availability.availableQuantity();
        var target =
                availableQuantity >= card.getQuantity()
                        ? DeckCard.OwnershipStatus.OWNED
                        : current == DeckCard.OwnershipStatus.PROXY
                                ? DeckCard.OwnershipStatus.PROXY
                                : DeckCard.OwnershipStatus.WISHLIST;
        if (target == current) {
            return Optional.empty();
        }
        card.setOwnershipStatus(target);
        deckCardRepository.save(card);
        return Optional.of(
                new OwnershipSyncResponse.OwnershipChange(
                        card.getId(), card.getCardPrintingId(), current.name(), target.name()));
    }

    private Map<Long, PhysicalCardAllocationService.CardAvailability> availabilityByDeckCardId(
            List<PhysicalCardAllocationService.CardAvailability> availability) {
        return availability.stream()
                .collect(
                        Collectors.toMap(
                                PhysicalCardAllocationService.CardAvailability::deckCardId,
                                Function.identity()));
    }

    private List<PhysicalCardAllocationService.DeckCardAvailabilityRequest> availabilityRequests(
            List<DeckCard> cards) {
        return cards.stream()
                .map(
                        card ->
                                new PhysicalCardAllocationService.DeckCardAvailabilityRequest(
                                        card.getId(), card.getCardPrintingId(), card.getQuantity()))
                .toList();
    }

    private int unavailableCount(
            List<PhysicalCardAllocationService.CardAvailability> availability) {
        return (int) availability.stream().filter(row -> row.missingQuantity() > 0).count();
    }

    private List<OwnershipSyncResponse.PhysicalAvailability> physicalAvailability(
            List<PhysicalCardAllocationService.CardAvailability> availability) {
        return availability.stream()
                .map(
                        row ->
                                new OwnershipSyncResponse.PhysicalAvailability(
                                        row.deckCardId(),
                                        row.cardPrintingId(),
                                        row.deckQuantity(),
                                        row.allocatedQuantity(),
                                        row.availableQuantity(),
                                        row.missingQuantity()))
                .toList();
    }
}
