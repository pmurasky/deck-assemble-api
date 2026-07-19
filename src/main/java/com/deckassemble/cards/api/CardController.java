package com.deckassemble.cards.api;

import com.deckassemble.cards.application.CardCatalogService;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardController {

  private final CardCatalogService cardCatalogService;

  public CardController(CardCatalogService cardCatalogService) {
    this.cardCatalogService = cardCatalogService;
  }

  @GetMapping
  public Page<CardSummaryResponse> search(@RequestParam(defaultValue = "") @Size(max = 100) String query,
      @RequestParam(required = false) String setCode,
      @RequestParam(required = false) String colorIdentity,
      @PageableDefault(size = 24) Pageable pageable) {
    return cardCatalogService.search(query, setCode, colorIdentity, pageable);
  }

  @GetMapping("/{cardId}")
  public CardDetailResponse getById(@PathVariable long cardId) {
    return cardCatalogService.getById(cardId);
  }

  @GetMapping("/{cardId}/printings")
  public List<CardPrintingResponse> getPrintings(@PathVariable long cardId) {
    return cardCatalogService.getPrintings(cardId);
  }
}
