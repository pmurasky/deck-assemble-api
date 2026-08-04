package com.deckassemble.collections.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionImportPreviewRepository
        extends JpaRepository<CollectionImportPreview, Long> {

    Optional<CollectionImportPreview> findByTokenAndProfileId(UUID token, Long profileId);

    Optional<CollectionImportPreview> findByProfileIdAndIdempotencyKey(
            Long profileId, String idempotencyKey);
}
