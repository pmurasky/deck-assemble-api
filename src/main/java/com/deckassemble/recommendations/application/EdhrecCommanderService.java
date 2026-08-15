package com.deckassemble.recommendations.application;

import com.deckassemble.recommendations.domain.EdhrecClient;
import com.deckassemble.recommendations.domain.EdhrecCommanderCache;
import com.deckassemble.recommendations.domain.EdhrecCommanderCacheRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class EdhrecCommanderService {

    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final EdhrecCommanderCacheRepository cacheRepository;
    private final EdhrecClient edhrecClient;
    private final ObjectMapper objectMapper;

    public EdhrecCommanderService(
            EdhrecCommanderCacheRepository cacheRepository,
            EdhrecClient edhrecClient,
            ObjectMapper objectMapper) {
        this.cacheRepository = cacheRepository;
        this.edhrecClient = edhrecClient;
        this.objectMapper = objectMapper;
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

    public Optional<Instant> fetchedAt(String commanderOracleId) {
        return cacheRepository
                .findByCommanderOracleId(commanderOracleId)
                .map(EdhrecCommanderCache::getFetchedAt);
    }

    /** Read-only freshness check; never triggers a network call. */
    public boolean hasFreshCache(String commanderOracleId) {
        return cacheRepository
                .findByCommanderOracleId(commanderOracleId)
                .map(this::isFresh)
                .orElse(false);
    }

    // Justified: method-local map, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<String, CardScore> getCardScores(String commanderOracleId, String commanderName) {
        var payload = getCommanderData(commanderOracleId, commanderName);
        Map<String, CardScore> scores = new HashMap<>();
        var cardlists =
                objectMapper
                        .readTree(payload)
                        .path("container")
                        .path("json_dict")
                        .path("cardlists");
        for (var cardlist : cardlists) {
            var header = cardlist.path("header").asString();
            for (var cardview : cardlist.path("cardviews")) {
                merge(scores, cardview, header);
            }
        }
        return scores;
    }

    private static void merge(Map<String, CardScore> scores, JsonNode cardview, String header) {
        var name = cardview.path("name").asString();
        if (name.isEmpty()) {
            return;
        }
        var existing = scores.get(name);
        var lists = new HashSet<String>();
        if (existing != null) {
            lists.addAll(existing.cardlists());
        }
        if (!header.isEmpty()) {
            lists.add(header);
        }
        scores.put(
                name,
                new CardScore(
                        max(existing == null ? null : existing.synergy(), synergyOf(cardview)),
                        max(existing == null ? null : existing.inclusion(), inclusionOf(cardview)),
                        lists));
    }

    private static @Nullable Double max(@Nullable Double left, @Nullable Double right) {
        if (left == null) {
            return right;
        }
        return right == null ? left : Math.max(left, right);
    }

    private static @Nullable Long max(@Nullable Long left, @Nullable Long right) {
        if (left == null) {
            return right;
        }
        return right == null ? left : Math.max(left, right);
    }

    private static @Nullable Double synergyOf(JsonNode cardview) {
        var value = cardview.path("synergy");
        return value.isNumber() ? value.doubleValue() : null;
    }

    private static @Nullable Long inclusionOf(JsonNode cardview) {
        var value = cardview.path("inclusion");
        return value.isNumber() ? value.longValue() : null;
    }

    static String toSlug(String commanderName) {
        return commanderName
                .toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
