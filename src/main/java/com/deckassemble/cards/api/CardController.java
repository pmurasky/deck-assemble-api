package com.deckassemble.cards.api;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardDetailResponse;
import com.deckassemble.cards.application.CardPrintingResponse;
import com.deckassemble.cards.application.CardSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardCatalogService cardCatalogService;

    public CardController(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    @GetMapping
    public Page<CardSummaryResponse> search(
            @Valid @ModelAttribute CardSearchRequest request,
            @PageableDefault(size = 24) Pageable pageable) {
        return cardCatalogService.search(
                request.toFilter(),
                request.ownedQuantityRange(),
                request.partnerForCardId(),
                pageable);
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
