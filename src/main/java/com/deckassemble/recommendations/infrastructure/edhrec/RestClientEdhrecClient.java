package com.deckassemble.recommendations.infrastructure.edhrec;

import com.deckassemble.recommendations.domain.EdhrecClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class RestClientEdhrecClient implements EdhrecClient {

    private final RestClient restClient;
    private final EdhrecRateLimiter rateLimiter;

    RestClientEdhrecClient(EdhrecProperties properties, EdhrecRateLimiter rateLimiter) {
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
    public String fetchCommanderData(String commanderSlug) {
        return get("/pages/commanders/{slug}.json", commanderSlug);
    }

    @Override
    public String fetchTopCommanders() {
        return get("/pages/commanders.json");
    }

    private String get(String uri, Object... uriVariables) {
        rateLimiter.awaitPermit();
        var payload = restClient.get().uri(uri, uriVariables).retrieve().body(String.class);
        if (payload == null) {
            throw new RestClientException("Empty response from EDHREC for " + uri);
        }
        return payload;
    }

    private SimpleClientHttpRequestFactory requestFactory(EdhrecProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
