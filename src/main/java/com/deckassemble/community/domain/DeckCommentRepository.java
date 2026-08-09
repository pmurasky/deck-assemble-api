package com.deckassemble.community.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCommentRepository extends JpaRepository<DeckComment, UUID> {

    List<DeckComment> findByDeckIdOrderByCreatedAtDesc(Long deckId);
}
