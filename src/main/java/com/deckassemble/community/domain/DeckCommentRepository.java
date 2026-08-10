package com.deckassemble.community.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCommentRepository extends JpaRepository<DeckComment, UUID> {

    List<DeckComment> findByDeckIdOrderByCreatedAtDesc(Long deckId);

    /** Visible (non-soft-deleted) comments for a deck, most recent first. */
    Page<DeckComment> findByDeckIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long deckId, Pageable pageable);

    /**
     * Comments a profile has posted since {@code since}, counting soft-deleted ones too — otherwise
     * delete-then-repost would bypass the rate limit in {@code CommentService}.
     */
    long countByProfileIdAndCreatedAtAfter(Long profileId, Instant since);
}
