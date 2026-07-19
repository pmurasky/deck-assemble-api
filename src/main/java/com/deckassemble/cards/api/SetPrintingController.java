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

@RestController
@RequestMapping("/sets")
public class SetPrintingController {

  private final CardCatalogService cardCatalogService;

  public SetPrintingController(CardCatalogService cardCatalogService) {
    this.cardCatalogService = cardCatalogService;
  }

  @GetMapping("/{setCode}/printings")
  public Page<CardSummaryResponse> getPrintings(@PathVariable String setCode,
      @RequestParam(defaultValue = "") @Size(max = 100) String query,
      @PageableDefault(size = 24, sort = "collectorNumber") Pageable pageable) {
    return cardCatalogService.getSetPrintings(setCode, query, pageable);
  }
}
