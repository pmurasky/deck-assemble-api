package com.deckassemble.collections.domain.physical;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollectionCardPhysicalMetadataRepository
        extends JpaRepository<CollectionCardPhysicalMetadata, Long> {

    Optional<CollectionCardPhysicalMetadata> findByCollectionCardId(Long collectionCardId);

    boolean existsByStorageLocationId(UUID storageLocationId);

    @Query(
            """
            select metadata from CollectionCardPhysicalMetadata metadata
            join CollectionCard card on card.id = metadata.collectionCardId
            where card.collectionId = :collectionId
            and (:locationId is null or metadata.storageLocationId = :locationId)
            and (:condition is null or metadata.condition = :condition)
            and (:language is null or metadata.language = :language)
            and (:finish is null or metadata.finish = :finish)
            order by metadata.collectionCardId
            """)
    List<CollectionCardPhysicalMetadata> findByCollectionIdAndFilters(
            @Param("collectionId") Long collectionId,
            @Param("locationId") @Nullable UUID locationId,
            @Param("condition") @Nullable CardCondition condition,
            @Param("language") @Nullable String language,
            @Param("finish") @Nullable PhysicalFinish finish);
}
