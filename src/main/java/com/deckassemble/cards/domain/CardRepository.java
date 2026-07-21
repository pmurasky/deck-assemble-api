package com.deckassemble.cards.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByScryfallOracleId(String scryfallOracleId);

    Page<Card> findByNameContainingIgnoreCaseAndActiveTrue(String query, Pageable pageable);
}
