package com.deckassemble.cards.infrastructure;

import com.deckassemble.cards.domain.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

  Optional<Card> findByScryfallOracleId(String scryfallOracleId);
}
