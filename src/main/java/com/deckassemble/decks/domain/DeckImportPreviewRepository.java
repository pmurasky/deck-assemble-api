package com.deckassemble.decks.domain;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DeckImportPreviewRepository extends JpaRepository<DeckImportPreview, Long> {

    Optional<DeckImportPreview> findByTokenAndProfileId(UUID token, Long profileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT preview FROM DeckImportPreview preview "
                    + "WHERE preview.token = :token AND preview.profileId = :profileId")
    Optional<DeckImportPreview> findLockedByTokenAndProfileId(UUID token, Long profileId);

    Optional<DeckImportPreview> findByProfileIdAndIdempotencyKey(
            Long profileId, String idempotencyKey);
}
