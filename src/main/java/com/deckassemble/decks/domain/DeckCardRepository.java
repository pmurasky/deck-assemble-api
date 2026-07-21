package com.deckassemble.decks.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCardRepository extends JpaRepository<DeckCard, Long> {

    List<DeckCard> findByDeckId(Long deckId);

    Optional<DeckCard> findByIdAndDeckId(Long id, Long deckId);

    Optional<DeckCard> findByDeckIdAndCardPrintingIdAndDeckSection(
            Long deckId, Long cardPrintingId, DeckCard.Section deckSection);
}
