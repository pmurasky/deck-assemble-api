package com.deckassemble.decks.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<Deck> findByIdAndProfileId(Long id, Long profileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT deck FROM Deck deck WHERE deck.id = :id AND deck.profileId = :profileId")
    Optional<Deck> findLockedByIdAndProfileId(Long id, Long profileId);
}
