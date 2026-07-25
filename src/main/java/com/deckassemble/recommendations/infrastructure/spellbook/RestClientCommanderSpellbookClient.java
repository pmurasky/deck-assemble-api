package com.deckassemble.recommendations.infrastructure.spellbook;

import com.deckassemble.recommendations.domain.CommanderSpellbookClient;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class RestClientCommanderSpellbookClient implements CommanderSpellbookClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    RestClientCommanderSpellbookClient(
            CommanderSpellbookProperties properties, ObjectMapper objectMapper) {
        restClient =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .defaultHeader("User-Agent", properties.userAgent())
                        .requestFactory(requestFactory(properties))
                        .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SpellbookCombo> findCombos(String deckList) {
        String payload =
                restClient
                        .post()
                        .uri("/find-my-combos?limit=20")
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(deckList)
                        .retrieve()
                        .body(String.class);
        if (payload == null) {
            throw new RestClientException("Empty response from Commander Spellbook");
        }
        return parse(payload);
    }

    List<SpellbookCombo> parse(String payload) {
        try {
            var combos = new ArrayList<SpellbookCombo>();
            for (JsonNode combo : objectMapper.readTree(payload).path("results").path("included")) {
                combos.add(toCombo(combo));
            }
            return combos;
        } catch (JacksonException exception) {
            throw new RestClientException("Invalid response from Commander Spellbook", exception);
        }
    }

    private SpellbookCombo toCombo(JsonNode combo) {
        return new SpellbookCombo(
                combo.path("id").asString(),
                cardNames(combo.path("uses")),
                featureNames(combo.path("produces")),
                combo.path("description").asString(),
                combo.path("notablePrerequisites").asString());
    }

    private List<String> cardNames(JsonNode uses) {
        var names = new ArrayList<String>();
        for (JsonNode use : uses) {
            names.add(use.path("card").path("name").asString());
        }
        return names;
    }

    private List<String> featureNames(JsonNode produces) {
        var names = new ArrayList<String>();
        for (JsonNode produced : produces) {
            names.add(produced.path("feature").path("name").asString());
        }
        return names;
    }

    private SimpleClientHttpRequestFactory requestFactory(CommanderSpellbookProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}
