package com.deckassemble.recommendations.application;

import com.deckassemble.recommendations.domain.EdhrecClient;
import com.deckassemble.recommendations.domain.EdhrecCommanderCache;
import com.deckassemble.recommendations.domain.EdhrecCommanderCacheRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class EdhrecCommanderService {

    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final EdhrecCommanderCacheRepository cacheRepository;
    private final EdhrecClient edhrecClient;

    public EdhrecCommanderService(
            EdhrecCommanderCacheRepository cacheRepository, EdhrecClient edhrecClient) {
        this.cacheRepository = cacheRepository;
        this.edhrecClient = edhrecClient;
    }

    @Transactional
    public String getCommanderData(String commanderOracleId, String commanderName) {
        var cached = cacheRepository.findByCommanderOracleId(commanderOracleId);
        if (cached.isPresent() && isFresh(cached.get())) {
            return cached.get().getPayload();
        }
        try {
            return fetchAndStore(commanderOracleId, commanderName, cached.orElse(null));
        } catch (RestClientException exception) {
            if (cached.isPresent()) {
                return cached.get().getPayload();
            }
            throw exception;
        }
    }

    private boolean isFresh(EdhrecCommanderCache entry) {
        return entry.getFetchedAt().isAfter(Instant.now().minus(CACHE_TTL));
    }

    private String fetchAndStore(
            String commanderOracleId,
            String commanderName,
            @Nullable EdhrecCommanderCache existing) {
        var payload = edhrecClient.fetchCommanderData(toSlug(commanderName));
        var fetchedAt = Instant.now();
        if (existing != null) {
            existing.refresh(payload, fetchedAt);
            cacheRepository.save(existing);
        } else {
            cacheRepository.save(new EdhrecCommanderCache(commanderOracleId, payload, fetchedAt));
        }
        return payload;
    }

    static String toSlug(String commanderName) {
        return commanderName
                .toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
