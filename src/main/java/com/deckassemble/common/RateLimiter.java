package com.deckassemble.common;

import java.time.Duration;
import java.time.Instant;

public class RateLimiter {

    private final Duration requestDelay;
    private final String targetName;
    private Instant nextRequestAt = Instant.MIN;

    public RateLimiter(Duration requestDelay, String targetName) {
        this.requestDelay = requestDelay;
        this.targetName = targetName;
    }

    public synchronized void awaitPermit() {
        var now = Instant.now();
        var delay = Duration.between(now, nextRequestAt);
        sleepIfNeeded(delay);
        nextRequestAt = Instant.now().plus(requestDelay);
    }

    private void sleepIfNeeded(Duration delay) {
        if (!delay.isPositive()) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while rate limiting " + targetName + " requests", exception);
        }
    }
}
