package com.deckassemble.cards.infrastructure.scryfall;

import com.deckassemble.common.RateLimiter;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
class ScryfallRateLimiter {

    private static final long MILLIS_PER_SECOND = 1_000L;

    private final RateLimiter rateLimiter;

    ScryfallRateLimiter(ScryfallProperties properties) {
        rateLimiter = new RateLimiter(properties.requestDelay(), "Scryfall");
    }

    void awaitPermit() {
        rateLimiter.awaitPermit();
    }

    long retryDelayMillis(RestClientException exception, long fallbackMillis) {
        return Optional.of(exception)
                .filter(RestClientResponseException.class::isInstance)
                .map(RestClientResponseException.class::cast)
                .map(RestClientResponseException::getResponseHeaders)
                .map(headers -> headers.getFirst(HttpHeaders.RETRY_AFTER))
                .map(ScryfallRateLimiter::parseRetryAfterMillis)
                .orElse(fallbackMillis);
    }

    private static @Nullable Long parseRetryAfterMillis(String retryAfter) {
        try {
            return Math.multiplyExact(Long.parseLong(retryAfter), MILLIS_PER_SECOND);
        } catch (ArithmeticException | NumberFormatException ignored) {
            return null;
        }
    }
}
