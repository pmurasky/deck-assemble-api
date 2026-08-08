package com.deckassemble.decks.domain.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckFolderRepository extends JpaRepository<DeckFolder, Long> {

    List<DeckFolder> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<DeckFolder> findByIdAndProfileId(Long id, Long profileId);

    boolean existsByProfileIdAndNameIgnoreCase(Long profileId, String name);
}
