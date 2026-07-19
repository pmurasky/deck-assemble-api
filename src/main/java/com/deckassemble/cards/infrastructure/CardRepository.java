package com.deckassemble.cards.infrastructure;

import com.deckassemble.cards.domain.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CardRepository extends JpaRepository<Card, Long> {

  Optional<Card> findByScryfallOracleId(String scryfallOracleId);

  Page<Card> findByNameContainingIgnoreCaseAndActiveTrue(String query, Pageable pageable);
}
