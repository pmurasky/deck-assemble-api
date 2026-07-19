package com.deckassemble.cards.infrastructure;

import com.deckassemble.cards.domain.MagicSet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagicSetRepository extends JpaRepository<MagicSet, Long> {

  Optional<MagicSet> findBySetCode(String setCode);

  Optional<MagicSet> findByScryfallSetId(String scryfallSetId);
}
