package com.deckassemble.imports.application;

import com.deckassemble.cards.application.BeginnerGuideStalenessService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class CardImportCardStore {

    private final CardRepository cardRepository;
    private final BeginnerGuideStalenessService beginnerGuideStalenessService;

    CardImportCardStore(
            CardRepository cardRepository,
            BeginnerGuideStalenessService beginnerGuideStalenessService) {
        this.cardRepository = cardRepository;
        this.beginnerGuideStalenessService = beginnerGuideStalenessService;
    }

    Optional<Card> findByScryfallOracleId(String oracleId) {
        return cardRepository.findByScryfallOracleId(oracleId);
    }

    Card save(Card card) {
        Card savedCard = cardRepository.save(card);
        beginnerGuideStalenessService.markStaleIfOracleChanged(savedCard);
        return savedCard;
    }
}
