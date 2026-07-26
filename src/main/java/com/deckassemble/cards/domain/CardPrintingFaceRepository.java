package com.deckassemble.cards.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardPrintingFaceRepository extends JpaRepository<CardPrintingFace, Long> {

    void deleteByCardPrintingId(Long cardPrintingId);
}
