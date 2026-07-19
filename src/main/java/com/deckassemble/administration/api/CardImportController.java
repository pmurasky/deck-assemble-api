package com.deckassemble.administration.api;

import com.deckassemble.cards.application.CardImportService;
import com.deckassemble.cards.application.ImportResult;
import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.imports.infrastructure.CardImportRunRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/card-imports")
public class CardImportController {

  private final CardImportService cardImportService;
  private final CardImportRunRepository cardImportRunRepository;

  public CardImportController(
      CardImportService cardImportService, CardImportRunRepository cardImportRunRepository) {
    this.cardImportService = cardImportService;
    this.cardImportRunRepository = cardImportRunRepository;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ImportResult importCards(@RequestParam @NotBlank String query) {
    return cardImportService.importQuery(query);
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<ImportRunHistoryResponse> history() {
    return cardImportRunRepository.findTop20ByOrderByStartedAtDesc().stream()
        .map(ImportRunHistoryResponse::from)
        .toList();
  }

  public record ImportRunHistoryResponse(
      long id,
      String provider,
      String query,
      String status,
      int recordsRead,
      int recordsCreated,
      int recordsUpdated,
      int recordsFailed,
      OffsetDateTime startedAt,
      OffsetDateTime completedAt) {

    static ImportRunHistoryResponse from(CardImportRun run) {
      return new ImportRunHistoryResponse(
          run.getId(),
          run.getProvider(),
          run.getQuery(),
          run.getStatus().name(),
          run.getRecordsRead(),
          run.getRecordsCreated(),
          run.getRecordsUpdated(),
          run.getRecordsFailed(),
          run.getStartedAt(),
          run.getCompletedAt());
    }
  }
}
