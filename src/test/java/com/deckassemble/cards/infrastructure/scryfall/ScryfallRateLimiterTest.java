package com.deckassemble.cards.infrastructure.scryfall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ScryfallRateLimiterTest {

    private static ScryfallRateLimiter limiterWithDelay(Duration delay) {
        return new ScryfallRateLimiter(
                new ScryfallProperties(
                        "https://api.scryfall.com",
                        "test-agent",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        delay));
    }

    @Test
    void shouldNotDelayFirstRequest() {
        var limiter = limiterWithDelay(Duration.ofSeconds(10));

        long start = System.nanoTime();
        limiter.awaitPermit();

        assertThat(System.nanoTime() - start).isLessThan(Duration.ofSeconds(1).toNanos());
    }

    @Test
    void shouldDelaySecondRequestWithinWindow() {
        var limiter = limiterWithDelay(Duration.ofMillis(150));

        limiter.awaitPermit();
        long start = System.nanoTime();
        limiter.awaitPermit();

        assertThat(System.nanoTime() - start)
                .isGreaterThanOrEqualTo(Duration.ofMillis(100).toNanos());
    }

    @Test
    void shouldRethrowAndPreserveInterruptWhenSleepInterrupted() {
        var limiter = limiterWithDelay(Duration.ofSeconds(10));
        limiter.awaitPermit();
        Thread.currentThread().interrupt();

        assertThatThrownBy(limiter::awaitPermit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Interrupted");
        // Thread.interrupted() reads and clears the flag so the worker thread stays clean.
        assertThat(Thread.interrupted()).isTrue();
    }
}
