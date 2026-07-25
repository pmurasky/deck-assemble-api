package com.deckassemble.recommendations.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckBuildRepository extends JpaRepository<DeckBuild, Long> {

    Optional<DeckBuild> findTopByDeckIdOrderByCreatedAtDesc(Long deckId);
}
