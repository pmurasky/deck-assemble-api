package com.deckassemble.collections.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionCardRepository extends JpaRepository<CollectionCard, Long> {

    List<CollectionCard> findByCollectionId(Long collectionId);

    Optional<CollectionCard> findByIdAndCollectionId(Long id, Long collectionId);

    Optional<CollectionCard> findByCollectionIdAndCardPrintingId(
            Long collectionId, Long cardPrintingId);
}
