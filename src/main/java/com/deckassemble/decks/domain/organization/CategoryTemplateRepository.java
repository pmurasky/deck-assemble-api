package com.deckassemble.decks.domain.organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryTemplateRepository extends JpaRepository<CategoryTemplate, Long> {

    List<CategoryTemplate> findByProfileIdOrderByNameAsc(Long profileId);

    Optional<CategoryTemplate> findByIdAndProfileId(Long id, Long profileId);

    boolean existsByProfileIdAndNameIgnoreCase(Long profileId, String name);
}
