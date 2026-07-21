package com.deckassemble.cards.infrastructure.scryfall;

import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallBulkData;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallList;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallSet;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class RestClientScryfallClient implements ScryfallClient {

    private static final ParameterizedTypeReference<ScryfallList<ScryfallSet>> SET_LIST =
            new ParameterizedTypeReference<>() {};
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
    public List<ScryfallSet> getSets() {
        return execute(() -> restClient.get().uri("/sets").retrieve().body(SET_LIST).data());
    }

    @Override
    public ScryfallList<ScryfallCard> searchCards(String query) {
        return execute(
                () ->
                        restClient
                                .get()
                                .uri(
                                        uriBuilder ->
                                                uriBuilder
                                                        .path("/cards/search")
                                                        .queryParam("q", query)
                                                        .queryParam("include_extras", true)
                                                        .queryParam("include_variations", true)
                                                        .queryParam("unique", "prints")
                                                        .build())
                                .retrieve()
                                .body(CARD_LIST));
    }

    @Override
    public ScryfallList<ScryfallCard> searchCards(URI uri) {
        return execute(() -> restClient.get().uri(uri).retrieve().body(CARD_LIST));
    }

    @Override
    public ScryfallBulkData getBulkData(String type) {
        return execute(
                () ->
                        restClient
                                .get()
                                .uri("/bulk-data/{type}", type)
                                .retrieve()
                                .body(ScryfallBulkData.class));
    }

    @Override
    public InputStream download(URI uri) {
        return execute(() -> restClient.get().uri(uri).retrieve().body(InputStream.class));
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
