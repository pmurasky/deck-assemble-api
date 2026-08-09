package com.deckassemble.community.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckFavoriteRepository extends JpaRepository<DeckFavorite, UUID> {

    List<DeckFavorite> findByProfileId(Long profileId);

    List<DeckFavorite> findByDeckId(Long deckId);

    boolean existsByProfileIdAndDeckId(Long profileId, Long deckId);
}
