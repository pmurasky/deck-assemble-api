package com.deckassemble.cards.infrastructure.scryfall;

import com.deckassemble.cards.domain.CardImportData;
import com.deckassemble.cards.domain.CardImportFace;
import com.deckassemble.cards.domain.CardImportImages;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardSearchPage;
import com.deckassemble.cards.domain.OracleTagIndex;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCardFace;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallImageUris;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallList;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallPrices;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
class RestClientScryfallClient implements ScryfallClient {

    private static final ParameterizedTypeReference<ScryfallList<ScryfallCard>> CARD_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ScryfallList<JsonNode>> RULING_LIST =
            new ParameterizedTypeReference<>() {};
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 500L;

    private final RestClient restClient;
    private final ScryfallRateLimiter rateLimiter;

    RestClientScryfallClient(ScryfallProperties properties, ScryfallRateLimiter rateLimiter) {
        restClient =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .defaultHeader("User-Agent", properties.userAgent())
                        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .requestFactory(requestFactory(properties))
                        .build();
        this.rateLimiter = rateLimiter;
    }

    @Override
    public CardSearchPage searchCards(String query) {
        return toPage(
                execute(
                        () ->
                                restClient
                                        .get()
                                        .uri(
                                                uriBuilder ->
                                                        uriBuilder
                                                                .path("/cards/search")
                                                                .queryParam("q", query)
                                                                .queryParam("include_extras", true)
                                                                .queryParam(
                                                                        "include_variations", true)
                                                                .queryParam("unique", "prints")
                                                                .build())
                                        .retrieve()
                                        .body(CARD_LIST)));
    }

    @Override
    public CardSearchPage searchCards(URI nextPageUri) {
        return toPage(execute(() -> restClient.get().uri(nextPageUri).retrieve().body(CARD_LIST)));
    }

    @Override
    public Map<String, Set<String>> fetchOracleTagAssignments() {
        URI downloadUri = oracleTagBulkUri();
        return execute(
                () ->
                        restClient
                                .get()
                                .uri(downloadUri)
                                .exchange(
                                        (request, response) -> {
                                            try (var decompressed =
                                                    new GZIPInputStream(response.getBody())) {
                                                return OracleTagIndex.parse(decompressed);
                                            }
                                        }));
    }

    private URI oracleTagBulkUri() {
        JsonNode manifest =
                execute(() -> restClient.get().uri("/bulk-data").retrieve().body(JsonNode.class));
        if (manifest == null) {
            throw new IllegalStateException("Scryfall bulk-data manifest was empty");
        }
        for (JsonNode entry : manifest.path("data")) {
            if ("oracle_tags".equals(entry.path("type").asString())) {
                return URI.create(entry.path("jsonl_download_uri").asString());
            }
        }
        throw new IllegalStateException("Scryfall bulk-data manifest has no oracle_tags entry");
    }

    @Override
    public CardPrice getCardPrice(String scryfallCardId) {
        ScryfallCard card =
                execute(
                        () ->
                                restClient
                                        .get()
                                        .uri("/cards/{id}", scryfallCardId)
                                        .retrieve()
                                        .body(ScryfallCard.class));
        return toPrice(card == null ? null : card.prices());
    }

    @Override
    public List<String> getRulings(String scryfallCardId) {
        var rulings =
                execute(
                        () ->
                                restClient
                                        .get()
                                        .uri("/cards/{id}/rulings", scryfallCardId)
                                        .retrieve()
                                        .body(RULING_LIST));
        return rulings.data().stream().map(ruling -> ruling.path("comment").asString()).toList();
    }

    private CardPrice toPrice(@Nullable ScryfallPrices prices) {
        if (prices == null) {
            return new CardPrice(null, null, null, null);
        }
        return new CardPrice(
                parse(prices.usd()),
                parse(prices.usdFoil()),
                parse(prices.eur()),
                parse(prices.tix()));
    }

    private static @Nullable BigDecimal parse(@Nullable String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private CardSearchPage toPage(ScryfallList<ScryfallCard> page) {
        return new CardSearchPage(
                page.data().stream().map(this::toImportData).toList(),
                page.hasMore(),
                page.nextPage());
    }

    // Suppressed: 30-field record factory mapping; splitting the constructor call harms
    // readability.
    @SuppressWarnings("checkstyle:MethodLength")
    private CardImportData toImportData(ScryfallCard source) {
        return new CardImportData(
                source.id(),
                source.oracleId(),
                source.name(),
                source.manaCost(),
                source.cmc(),
                source.typeLine(),
                source.oracleText(),
                source.power(),
                source.toughness(),
                source.loyalty(),
                source.colors(),
                source.colorIdentity(),
                source.keywords(),
                source.layout(),
                source.reserved(),
                source.setId(),
                source.set(),
                source.setName(),
                source.collectorNumber(),
                source.rarity(),
                source.artist(),
                source.flavorText(),
                source.flavorName(),
                toImages(imageUris(source)),
                toFaces(source.cardFaces()),
                source.releasedAt(),
                source.foil(),
                source.nonfoil(),
                source.promo(),
                source.digital(),
                source.lang(),
                source.legalities(),
                source.gameChanger());
    }

    private @Nullable CardImportImages toImages(@Nullable ScryfallImageUris imageUris) {
        if (imageUris == null) {
            return null;
        }
        return new CardImportImages(imageUris.small(), imageUris.normal(), imageUris.large());
    }

    private List<CardImportFace> toFaces(@Nullable List<ScryfallCardFace> cardFaces) {
        if (cardFaces == null) {
            return List.of();
        }
        return cardFaces.stream().map(this::toImportFace).toList();
    }

    private CardImportFace toImportFace(ScryfallCardFace face) {
        return new CardImportFace(
                face.name(),
                face.manaCost(),
                face.typeLine(),
                face.oracleText(),
                face.power(),
                face.toughness(),
                face.loyalty(),
                face.colors(),
                face.imageUris() == null ? null : face.imageUris().normal());
    }

    private @Nullable ScryfallImageUris imageUris(ScryfallCard source) {
        if (source.imageUris() != null) {
            return source.imageUris();
        }
        if (source.cardFaces() == null) {
            return null;
        }
        return source.cardFaces().stream()
                .map(face -> face.imageUris())
                .filter(uri -> uri != null)
                .findFirst()
                .orElse(null);
    }

    private SimpleClientHttpRequestFactory requestFactory(ScryfallProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }

    private <T> T execute(Supplier<T> request) {
        for (var attempt = 1; ; attempt++) {
            rateLimiter.awaitPermit();
            try {
                return request.get();
            } catch (RestClientException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                pauseBeforeRetry(exception, attempt);
            }
        }
    }

    private void pauseBeforeRetry(RestClientException exception, int attempt) {
        var delayMillis =
                rateLimiter.retryDelayMillis(exception, BASE_BACKOFF_MILLIS << (attempt - 1));
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while retrying Scryfall request", interrupted);
        }
    }
}
