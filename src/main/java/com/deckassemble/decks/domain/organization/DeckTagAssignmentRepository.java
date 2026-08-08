package com.deckassemble.decks.domain.organization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckTagAssignmentRepository extends JpaRepository<DeckTagAssignment, Long> {

    List<DeckTagAssignment> findByDeckId(Long deckId);

    List<DeckTagAssignment> findByTagId(Long tagId);

    void deleteByDeckId(Long deckId);

    void deleteByTagId(Long tagId);
}
