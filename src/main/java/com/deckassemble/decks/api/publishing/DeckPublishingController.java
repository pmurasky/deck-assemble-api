package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.application.publishing.DeckPrimerService;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-controlled deck visibility and Markdown primer, and the anonymous-reachable shared view by
 * slug.
 */
@RestController
public class DeckPublishingController {

    private final DeckPublishingService deckPublishingService;
    private final DeckPrimerService deckPrimerService;

    public DeckPublishingController(
            DeckPublishingService deckPublishingService, DeckPrimerService deckPrimerService) {
        this.deckPublishingService = deckPublishingService;
        this.deckPrimerService = deckPrimerService;
    }

    @PatchMapping("/decks/{deckId}/publishing")
    public SharedDeckResponse updateVisibility(
            @PathVariable long deckId, @Valid @RequestBody DeckPublishingRequest request) {
        return SharedDeckResponse.from(
                deckPublishingService.updateVisibility(deckId, request.visibility()));
    }

    @PutMapping("/decks/{deckId}/primer")
    public DeckPrimerResponse updatePrimer(
            @PathVariable long deckId, @Valid @RequestBody DeckPrimerRequest request) {
        return DeckPrimerResponse.from(
                deckPrimerService.updatePrimer(deckId, request.title(), request.markdownSource()));
    }

    @GetMapping("/shared/decks/{slug}")
    public SharedDeckResponse shared(@PathVariable String slug) {
        return SharedDeckResponse.from(deckPublishingService.getShared(slug));
    }
}
