package com.deckassemble.cards.infrastructure;

import com.deckassemble.cards.domain.CardFace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardFaceRepository extends JpaRepository<CardFace, Long> {

    List<CardFace> findByCardIdOrderByFaceOrderAsc(Long cardId);
}
