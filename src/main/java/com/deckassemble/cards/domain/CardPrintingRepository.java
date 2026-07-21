package com.deckassemble.cards.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardPrintingRepository extends JpaRepository<CardPrinting, Long> {

    List<CardPrinting> findByCardIdOrderByReleasedAtDesc(Long cardId);

    List<CardPrinting> findByMagicSetIdOrderByCollectorNumberAsc(Long magicSetId);

    Page<CardPrinting> findByMagicSetSetCodeAndActiveTrueAndCardActiveTrue(
            String setCode, Pageable pageable);

    Page<CardPrinting>
            findByMagicSetSetCodeAndActiveTrueAndCardActiveTrueAndCardNameContainingIgnoreCase(
                    String setCode, String query, Pageable pageable);

    Optional<CardPrinting> findByScryfallCardId(String scryfallCardId);
}
