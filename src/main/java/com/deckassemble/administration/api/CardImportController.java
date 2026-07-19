package com.deckassemble.administration.api;

import com.deckassemble.cards.application.CardImportService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/card-imports")
public class CardImportController {

  private final CardImportService cardImportService;

  public CardImportController(CardImportService cardImportService) {
    this.cardImportService = cardImportService;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public CardImportResponse importCards(@RequestParam @NotBlank String query) {
    return new CardImportResponse(cardImportService.importQuery(query));
  }

  public record CardImportResponse(int importedCount) {}
}
