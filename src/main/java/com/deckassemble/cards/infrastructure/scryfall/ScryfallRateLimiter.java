package com.deckassemble.cards.infrastructure.scryfall;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class ScryfallRateLimiter {

  private final Duration requestDelay;
  private Instant nextRequestAt = Instant.MIN;

  ScryfallRateLimiter(ScryfallProperties properties) {
    requestDelay = properties.requestDelay();
  }

  synchronized void awaitPermit() {
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
      throw new IllegalStateException("Interrupted while rate limiting Scryfall requests", exception);
    }
  }
}
