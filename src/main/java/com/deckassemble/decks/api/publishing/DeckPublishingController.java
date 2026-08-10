package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.application.publishing.DeckForkService;
import com.deckassemble.decks.application.publishing.DeckPrimerService;
import com.deckassemble.decks.application.publishing.DeckPublishingService;
import com.deckassemble.decks.domain.Deck;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Owner-controlled deck visibility, publishing, and Markdown primer, plus the anonymous-reachable
 * shared view and fork by slug.
 */
@RestController
public class DeckPublishingController {

    private final DeckPublishingService deckPublishingService;
    private final DeckPrimerService deckPrimerService;
    private final DeckForkService deckForkService;

    public DeckPublishingController(
            DeckPublishingService deckPublishingService,
            DeckPrimerService deckPrimerService,
            DeckForkService deckForkService) {
        this.deckPublishingService = deckPublishingService;
        this.deckPrimerService = deckPrimerService;
        this.deckForkService = deckForkService;
    }

    @PatchMapping("/decks/{deckId}/publishing")
    public SharedDeckResponse updateVisibility(
            @PathVariable long deckId, @Valid @RequestBody DeckPublishingRequest request) {
        return SharedDeckResponse.from(
                deckPublishingService.updateVisibility(deckId, request.visibility()));
    }

    @PostMapping("/decks/{deckId}/publish")
    public SharedDeckResponse publish(@PathVariable long deckId) {
        return SharedDeckResponse.from(deckPublishingService.publish(deckId));
    }

    @PatchMapping("/decks/{deckId}/comments-enabled")
    public SharedDeckResponse setCommentsEnabled(
            @PathVariable long deckId, @Valid @RequestBody DeckCommentsEnabledRequest request) {
        return SharedDeckResponse.from(
                deckPublishingService.setCommentsEnabled(deckId, request.enabled()));
    }

    @PutMapping("/decks/{deckId}/primer")
    public DeckPrimerResponse updatePrimer(
            @PathVariable long deckId, @Valid @RequestBody DeckPrimerRequest request) {
        DeckPrimerService.PrimerResult result =
                deckPrimerService.updatePrimer(
                        deckId,
                        request.title(),
                        request.markdownSource(),
                        request.expectedRevision());
        return DeckPrimerResponse.from(result.deck(), result.revisionNumber());
    }

    @GetMapping("/shared/decks/{slug}")
    public SharedDeckResponse shared(@PathVariable String slug) {
        DeckPublishingService.SharedDeckView view = deckPublishingService.getShared(slug);
        return SharedDeckResponse.from(view.deck(), view.pinnedSnapshot());
    }

    @PostMapping("/shared/decks/{slug}/fork")
    public ResponseEntity<DeckForkResponse> fork(@PathVariable String slug) {
        Deck forked = deckForkService.fork(slug);
        return ResponseEntity.created(URI.create("/api/v1/decks/" + forked.getId()))
                .body(DeckForkResponse.from(forked));
    }
}
