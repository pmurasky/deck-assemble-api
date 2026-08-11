package com.deckassemble.collections.application.physical;

import java.util.List;

public interface PhysicalDeckLookup {

    void owned(long deckId);

    void ownedLocked(long deckId);

    DeckCardView deckCard(long deckId, long deckCardId);

    List<DeckCardView> deckCards(long deckId);

    record DeckCardView(long id, long deckId, long cardPrintingId, int quantity) {}
}
