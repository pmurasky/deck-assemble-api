package com.deckassemble.decks.application;

import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Syncs deck card ownership with the user's collection and acquires wishlist cards. */
@Service
@Transactional
public class DeckOwnershipService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;
    private final DeckCardService deckCardService;
    private final OwnershipChecker ownershipChecker;
    private final CollectionService collectionService;

    public DeckOwnershipService(
            DeckAccessGuard deckAccessGuard,
            DeckCardRepository deckCardRepository,
            DeckCardService deckCardService,
            OwnershipChecker ownershipChecker,
            CollectionService collectionService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
        this.deckCardService = deckCardService;
        this.ownershipChecker = ownershipChecker;
        this.collectionService = collectionService;
    }

    public OwnershipSyncResponse syncOwnership(long deckId) {
        deckAccessGuard.owned(deckId);
        List<DeckCard> cards = deckCardRepository.findByDeckId(deckId);
        var ownedPrintingIds =
                ownershipChecker.filterOwnedPrintingIds(
                        deckAccessGuard.profileId(),
                        cards.stream().map(DeckCard::getCardPrintingId).toList());
        var changes =
                cards.stream()
                        .map(card -> syncCard(card, ownedPrintingIds))
                        .flatMap(Optional::stream)
                        .toList();
        return new OwnershipSyncResponse(changes.size(), changes);
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
            DeckCard card, Set<Long> ownedPrintingIds) {
        var current = card.getOwnershipStatus();
        var target =
                ownedPrintingIds.contains(card.getCardPrintingId())
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
}
