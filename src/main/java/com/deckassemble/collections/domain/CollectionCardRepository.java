package com.deckassemble.collections.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionCardRepository extends JpaRepository<CollectionCard, Long> {

    @Query("select distinct c.cardPrintingId from CollectionCard c")
    Set<Long> findDistinctCardPrintingIds();

    List<CollectionCard> findByCollectionId(Long collectionId);

    List<CollectionCard> findByCollectionIdIn(Collection<Long> collectionIds);

    Optional<CollectionCard> findByIdAndCollectionId(Long id, Long collectionId);

    Optional<CollectionCard> findByCollectionIdAndCardPrintingId(
            Long collectionId, Long cardPrintingId);

    @Query(
            """
            select card from CollectionCard card
            join CardCollection collection on collection.id = card.collectionId
            join CardPrinting ownedPrinting on ownedPrinting.id = card.cardPrintingId
            join CardPrinting requestedPrinting on requestedPrinting.id = :cardPrintingId
            where collection.profileId = :profileId
            and ownedPrinting.card.scryfallOracleId = requestedPrinting.card.scryfallOracleId
            order by case when card.cardPrintingId = :cardPrintingId then 0 else 1 end, card.id
            """)
    List<CollectionCard> findCompatibleOwnedCards(
            @Param("profileId") Long profileId, @Param("cardPrintingId") Long cardPrintingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select card from CollectionCard card
            join CardCollection collection on collection.id = card.collectionId
            join CardPrinting ownedPrinting on ownedPrinting.id = card.cardPrintingId
            join CardPrinting requestedPrinting on requestedPrinting.id = :cardPrintingId
            where collection.profileId = :profileId
            and ownedPrinting.card.scryfallOracleId = requestedPrinting.card.scryfallOracleId
            order by case when card.cardPrintingId = :cardPrintingId then 0 else 1 end, card.id
            """)
    List<CollectionCard> findCompatibleOwnedCardsLocked(
            @Param("profileId") Long profileId, @Param("cardPrintingId") Long cardPrintingId);
}
