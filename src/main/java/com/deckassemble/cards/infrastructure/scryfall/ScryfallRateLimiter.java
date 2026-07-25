package com.deckassemble.cards.infrastructure.scryfall;

import com.deckassemble.common.RateLimiter;
import org.springframework.stereotype.Component;

@Component
class ScryfallRateLimiter {

    private final RateLimiter rateLimiter;

    ScryfallRateLimiter(ScryfallProperties properties) {
        rateLimiter = new RateLimiter(properties.requestDelay(), "Scryfall");
    }

    void awaitPermit() {
        rateLimiter.awaitPermit();
    }
}
