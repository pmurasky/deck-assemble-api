package com.deckassemble.cards.api;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardSummaryResponse;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/printings")
public class PrintingController {

    private final CardCatalogService cardCatalogService;

    public PrintingController(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    // Suppressed: each parameter is an independent optional filter exposed as a query parameter;
    // grouping them would change the public API shape for no cohesion gain.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    @GetMapping
    public Page<CardSummaryResponse> search(
            @RequestParam(defaultValue = "") @Size(max = 100) String query,
            @RequestParam(required = false) @Nullable String setCode,
            @RequestParam(required = false) @Nullable String colorIdentity,
            @RequestParam(required = false) @Nullable String type,
            @RequestParam(required = false) @Nullable Boolean commanderEligible,
            @RequestParam(required = false) @Nullable Long partnerForCardId,
            @PageableDefault(size = 24) Pageable pageable) {
        return cardCatalogService.searchPrintings(
                query, setCode, colorIdentity, type, commanderEligible, partnerForCardId, pageable);
    }
}
