package com.deckassemble.collections.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CollectionCardRepository extends JpaRepository<CollectionCard, Long> {

    @Query("select distinct c.cardPrintingId from CollectionCard c")
    Set<Long> findDistinctCardPrintingIds();

    List<CollectionCard> findByCollectionId(Long collectionId);

    List<CollectionCard> findByCollectionIdIn(Collection<Long> collectionIds);

    Optional<CollectionCard> findByIdAndCollectionId(Long id, Long collectionId);

    Optional<CollectionCard> findByCollectionIdAndCardPrintingId(
            Long collectionId, Long cardPrintingId);
}
