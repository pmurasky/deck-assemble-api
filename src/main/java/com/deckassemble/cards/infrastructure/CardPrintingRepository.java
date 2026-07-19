package com.deckassemble.cards.infrastructure;

import com.deckassemble.cards.domain.CardPrinting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardPrintingRepository extends JpaRepository<CardPrinting, Long> {

  List<CardPrinting> findByCardIdOrderByReleasedAtDesc(Long cardId);

  List<CardPrinting> findByMagicSetIdOrderByCollectorNumberAsc(Long magicSetId);

  Optional<CardPrinting> findByScryfallCardId(String scryfallCardId);
}
