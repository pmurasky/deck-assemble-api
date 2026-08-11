package com.deckassemble.community.api;

import com.deckassemble.community.application.DeckDiscoveryService;
import com.deckassemble.decks.application.DeckAccessGuard;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeckDiscoveryController {

    private final DeckDiscoveryService deckDiscoveryService;
    private final DeckAccessGuard deckAccessGuard;

    public DeckDiscoveryController(
            DeckDiscoveryService deckDiscoveryService, DeckAccessGuard deckAccessGuard) {
        this.deckDiscoveryService = deckDiscoveryService;
        this.deckAccessGuard = deckAccessGuard;
    }

    @GetMapping("/community/decks")
    public DeckDiscoveryResponse discover(
            @ModelAttribute DeckDiscoveryQuery query,
            @PageableDefault(size = 20, sort = "updated") Pageable pageable) {
        return DeckDiscoveryResponse.from(
                deckDiscoveryService.discover(
                        new DeckDiscoveryService.Query(
                                query.getCommander(),
                                query.getColors(),
                                query.getTags(),
                                query.getCategory(),
                                query.getUpdatedAfter(),
                                query.getUpdatedBefore(),
                                query.getFavorited()),
                        pageable));
    }

    @GetMapping("/community/feed")
    public DeckDiscoveryResponse feed(@PageableDefault(size = 20) Pageable pageable) {
        return DeckDiscoveryResponse.from(
                deckDiscoveryService.feed(deckAccessGuard.profileId(), pageable));
    }
}
