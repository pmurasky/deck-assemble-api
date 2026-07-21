package com.deckassemble.cards.infrastructure.scryfall;

import com.deckassemble.cards.domain.CardImportData;
import com.deckassemble.cards.domain.CardImportImages;
import com.deckassemble.cards.domain.CardSearchPage;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallImageUris;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallList;
import java.net.URI;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class RestClientScryfallClient implements ScryfallClient {

    private static final ParameterizedTypeReference<ScryfallList<ScryfallCard>> CARD_LIST =
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
                toImages(imageUris(source)),
                source.releasedAt(),
                source.foil(),
                source.nonfoil(),
                source.promo(),
                source.digital(),
                source.lang(),
                source.legalities());
    }

    private @Nullable CardImportImages toImages(@Nullable ScryfallImageUris imageUris) {
        if (imageUris == null) {
            return null;
        }
        return new CardImportImages(imageUris.small(), imageUris.normal(), imageUris.large());
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
                pauseBeforeRetry(attempt);
            }
        }
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(BASE_BACKOFF_MILLIS << (attempt - 1));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while retrying Scryfall request", exception);
        }
    }
}
