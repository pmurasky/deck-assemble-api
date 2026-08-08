package com.deckassemble.decks.domain.organization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCategoryAssignmentRepository
        extends JpaRepository<DeckCategoryAssignment, Long> {

    List<DeckCategoryAssignment> findByDeckCategoryIdIn(List<Long> deckCategoryIds);

    void deleteByDeckCategoryId(Long deckCategoryId);
}
