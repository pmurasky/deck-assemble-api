package com.deckassemble.cards.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Optional<Card> findByScryfallOracleId(String scryfallOracleId);

    Page<Card> findByNameContainingIgnoreCaseAndActiveTrue(String query, Pageable pageable);

    List<Card> findByNameIn(Collection<String> names);

    List<Card> findByScryfallOracleIdIn(Collection<String> scryfallOracleIds);

    @Modifying
    @Query("UPDATE Card c SET c.commanderRank = NULL")
    void clearCommanderRanks();

    @Modifying
    @Query("UPDATE Card c SET c.gameChanger = FALSE")
    void clearGameChangers();
}
