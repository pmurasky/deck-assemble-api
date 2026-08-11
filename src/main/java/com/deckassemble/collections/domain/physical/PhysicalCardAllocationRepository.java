package com.deckassemble.collections.domain.physical;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhysicalCardAllocationRepository
        extends JpaRepository<PhysicalCardAllocation, Long> {

    List<PhysicalCardAllocation> findByDeckIdOrderById(Long deckId);

    Optional<PhysicalCardAllocation> findByIdAndDeckId(Long id, Long deckId);

    Optional<PhysicalCardAllocation> findByDeckCardIdAndCollectionCardId(
            Long deckCardId, Long collectionCardId);

    @Modifying
    void deleteByDeckId(Long deckId);

    @Query(
            """
            select coalesce(sum(allocation.quantity), 0)
            from PhysicalCardAllocation allocation
            where allocation.deckCardId = :deckCardId
            and (:excludedAllocationId is null or allocation.id <> :excludedAllocationId)
            """)
    int sumByDeckCardIdExcluding(
            @Param("deckCardId") Long deckCardId,
            @Param("excludedAllocationId") @Nullable Long excludedAllocationId);

    @Query(
            """
            select allocation.collectionCardId as collectionCardId,
                   coalesce(sum(allocation.quantity), 0) as quantity
            from PhysicalCardAllocation allocation
            where allocation.collectionCardId in :collectionCardIds
            and (:excludedAllocationId is null or allocation.id <> :excludedAllocationId)
            group by allocation.collectionCardId
            """)
    List<CollectionCardAllocationTotal> sumByCollectionCardIdsExcluding(
            @Param("collectionCardIds") Collection<Long> collectionCardIds,
            @Param("excludedAllocationId") @Nullable Long excludedAllocationId);

    interface CollectionCardAllocationTotal {
        Long getCollectionCardId();

        int getQuantity();
    }
}
