package com.deckassemble.decks.domain.collaboration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCollaboratorRepository extends JpaRepository<DeckCollaborator, UUID> {

    List<DeckCollaborator> findByDeckId(Long deckId);

    Optional<DeckCollaborator> findByDeckIdAndProfileId(Long deckId, Long profileId);
}
