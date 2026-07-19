package com.deckassemble.cards.application;

import com.deckassemble.cards.api.CardDetailResponse;
import com.deckassemble.cards.api.CardSummaryResponse;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.cards.api.CardPrintingResponse;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CardCatalogService {

  private final CardRepository cardRepository;
  private final CardPrintingRepository cardPrintingRepository;

  public CardCatalogService(CardRepository cardRepository, CardPrintingRepository cardPrintingRepository) {
    this.cardRepository = cardRepository;
    this.cardPrintingRepository = cardPrintingRepository;
  }

  public Page<CardSummaryResponse> search(String query, Pageable pageable) {
    return cardRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable)
        .map(CardSummaryResponse::from);
  }

  public CardDetailResponse getById(long cardId) {
    return cardRepository.findById(cardId).filter(card -> card.getActive())
        .map(CardDetailResponse::from).orElseThrow(CardNotFoundException::new);
  }

  public List<CardPrintingResponse> getPrintings(long cardId) {
    getById(cardId);
    return cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
        .map(CardPrintingResponse::from).toList();
  }
}
