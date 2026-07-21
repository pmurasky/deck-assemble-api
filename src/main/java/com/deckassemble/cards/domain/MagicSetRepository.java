package com.deckassemble.cards.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagicSetRepository extends JpaRepository<MagicSet, Long> {

    Optional<MagicSet> findBySetCode(String setCode);

    Optional<MagicSet> findByScryfallSetId(String scryfallSetId);
}
