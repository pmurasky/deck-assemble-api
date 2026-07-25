package com.deckassemble.recommendations.infrastructure.edhrec;

import com.deckassemble.common.RateLimiter;
import org.springframework.stereotype.Component;

@Component
class EdhrecRateLimiter {

    private final RateLimiter rateLimiter;

    EdhrecRateLimiter(EdhrecProperties properties) {
        rateLimiter = new RateLimiter(properties.requestDelay(), "EDHREC");
    }

    void awaitPermit() {
        rateLimiter.awaitPermit();
    }
}
