package com.deckassemble.decks.domain.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCategoryRepository extends JpaRepository<DeckCategory, Long> {

    List<DeckCategory> findByDeckIdOrderByDisplayOrderAscIdAsc(Long deckId);

    Optional<DeckCategory> findByIdAndDeckId(Long id, Long deckId);

    boolean existsByDeckId(Long deckId);

    boolean existsByDeckIdAndName(Long deckId, String name);

    long countByDeckId(Long deckId);
}
