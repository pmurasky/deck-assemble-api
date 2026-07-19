package com.deckassemble.cards.application;

import com.deckassemble.cards.api.CardDetailResponse;
import com.deckassemble.cards.api.CardSummaryResponse;
import com.deckassemble.cards.infrastructure.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CardCatalogService {

  private final CardRepository cardRepository;

  public CardCatalogService(CardRepository cardRepository) {
    this.cardRepository = cardRepository;
  }

  public Page<CardSummaryResponse> search(String query, Pageable pageable) {
    return cardRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable)
        .map(CardSummaryResponse::from);
  }

  public CardDetailResponse getById(long cardId) {
    return cardRepository.findById(cardId).filter(card -> card.getActive())
        .map(CardDetailResponse::from).orElseThrow(CardNotFoundException::new);
  }
}
