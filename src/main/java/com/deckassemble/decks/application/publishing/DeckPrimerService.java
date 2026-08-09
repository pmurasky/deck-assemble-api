package com.deckassemble.decks.application.publishing;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeckPrimerService {

    private final DeckRepository deckRepository;
    private final DeckAccessGuard deckAccessGuard;

    public DeckPrimerService(DeckRepository deckRepository, DeckAccessGuard deckAccessGuard) {
        this.deckRepository = deckRepository;
        this.deckAccessGuard = deckAccessGuard;
    }

    public Deck updatePrimer(long deckId, String title, String markdownSource) {
        Deck deck = deckAccessGuard.owned(deckId);
        deck.setPrimerTitle(title);
        deck.setPrimerMarkdown(markdownSource);
        return deckRepository.save(deck);
    }
}
