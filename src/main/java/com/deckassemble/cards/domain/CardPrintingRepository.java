package com.deckassemble.cards.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardPrintingRepository
        extends JpaRepository<CardPrinting, Long>, JpaSpecificationExecutor<CardPrinting> {

    List<CardPrinting> findByCardIdOrderByReleasedAtDesc(Long cardId);

    List<CardPrinting> findByMagicSetIdOrderByCollectorNumberAsc(Long magicSetId);

    Page<CardPrinting> findByMagicSetSetCodeAndActiveTrueAndCardActiveTrue(
            String setCode, Pageable pageable);

    Page<CardPrinting>
            findByMagicSetSetCodeAndActiveTrueAndCardActiveTrueAndCardNameContainingIgnoreCase(
                    String setCode, String query, Pageable pageable);

    Optional<CardPrinting> findByScryfallCardId(String scryfallCardId);

    @Query(
            """
            SELECT printing FROM CardPrinting printing
            WHERE LOWER(printing.magicSet.setCode) = LOWER(:setCode)
              AND LOWER(printing.collectorNumber) = LOWER(:collectorNumber)
              AND (LOWER(printing.card.name) = LOWER(:name)
                   OR LOWER(printing.flavorName) = LOWER(:name))
            """)
    List<CardPrinting> findExactPrintingReference(
            @Param("name") String name,
            @Param("setCode") String setCode,
            @Param("collectorNumber") String collectorNumber);
}
