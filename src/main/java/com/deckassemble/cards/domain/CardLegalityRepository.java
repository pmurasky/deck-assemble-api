package com.deckassemble.cards.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardLegalityRepository extends JpaRepository<CardLegality, Long> {

    List<CardLegality> findByCardId(Long cardId);
}
