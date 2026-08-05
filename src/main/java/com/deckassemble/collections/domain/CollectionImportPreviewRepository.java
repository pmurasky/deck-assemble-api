package com.deckassemble.collections.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CollectionImportPreviewRepository
        extends JpaRepository<CollectionImportPreview, Long> {

    Optional<CollectionImportPreview> findByTokenAndProfileId(UUID token, Long profileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT preview FROM CollectionImportPreview preview "
                    + "WHERE preview.token = :token AND preview.profileId = :profileId")
    Optional<CollectionImportPreview> findLockedByTokenAndProfileId(UUID token, Long profileId);

    Optional<CollectionImportPreview> findByProfileIdAndIdempotencyKey(
            Long profileId, String idempotencyKey);
}
