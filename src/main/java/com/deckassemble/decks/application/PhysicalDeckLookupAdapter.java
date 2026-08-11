package com.deckassemble.decks.application;

import com.deckassemble.collections.application.physical.PhysicalDeckLookup;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PhysicalDeckLookupAdapter implements PhysicalDeckLookup {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;

    public PhysicalDeckLookupAdapter(
            DeckAccessGuard deckAccessGuard, DeckCardRepository deckCardRepository) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
    }

    @Override
    public void owned(long deckId) {
        deckAccessGuard.owned(deckId);
    }

    @Override
    public void ownedLocked(long deckId) {
        deckAccessGuard.ownedLocked(deckId);
    }

    @Override
    public DeckCardView deckCard(long deckId, long deckCardId) {
        return deckCardRepository
                .findByIdAndDeckId(deckCardId, deckId)
                .map(this::view)
                .orElseThrow(DeckCardNotFoundException::new);
    }

    @Override
    public List<DeckCardView> deckCards(long deckId) {
        return deckCardRepository.findByDeckId(deckId).stream().map(this::view).toList();
    }

    private DeckCardView view(DeckCard card) {
        return new DeckCardView(
                card.getId(), card.getDeckId(), card.getCardPrintingId(), card.getQuantity());
    }
}
