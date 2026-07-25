package com.deckassemble.cards.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardPriceSnapshotRepository extends JpaRepository<CardPriceSnapshot, Long> {

    @Query(
            """
            select s from CardPriceSnapshot s
            where s.cardPrintingId in :printingIds
            and s.fetchedAt = (
                select max(other.fetchedAt) from CardPriceSnapshot other
                where other.cardPrintingId = s.cardPrintingId)
            """)
    List<CardPriceSnapshot> findLatestByCardPrintingIds(
            @Param("printingIds") Collection<Long> printingIds);

    @Query("select distinct s.cardPrintingId from CardPriceSnapshot s")
    Set<Long> findTrackedPrintingIds();
}
