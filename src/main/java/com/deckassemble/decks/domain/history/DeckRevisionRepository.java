package com.deckassemble.decks.domain.history;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRevisionRepository extends JpaRepository<DeckRevision, Long> {

    List<DeckRevision> findByDeckIdOrderByRevisionNumberDesc(Long deckId);

    Page<DeckRevision> findByDeckIdOrderByRevisionNumberDesc(Long deckId, Pageable pageable);

    Optional<DeckRevision> findByDeckIdAndRevisionNumber(Long deckId, int revisionNumber);

    Optional<DeckRevision> findFirstByDeckIdOrderByRevisionNumberDesc(Long deckId);
}
