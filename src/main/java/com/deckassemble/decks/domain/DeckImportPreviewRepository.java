package com.deckassemble.decks.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckImportPreviewRepository extends JpaRepository<DeckImportPreview, Long> {

    Optional<DeckImportPreview> findByTokenAndProfileId(UUID token, Long profileId);

    Optional<DeckImportPreview> findByProfileIdAndIdempotencyKey(
            Long profileId, String idempotencyKey);
}
