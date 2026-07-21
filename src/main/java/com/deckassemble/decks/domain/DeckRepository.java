package com.deckassemble.decks.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<Deck> findByIdAndProfileId(Long id, Long profileId);
}
