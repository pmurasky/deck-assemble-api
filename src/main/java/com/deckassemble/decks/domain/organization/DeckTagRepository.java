package com.deckassemble.decks.domain.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckTagRepository extends JpaRepository<DeckTag, Long> {

    List<DeckTag> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<DeckTag> findByIdAndProfileId(Long id, Long profileId);

    boolean existsByProfileIdAndNameIgnoreCase(Long profileId, String name);
}
