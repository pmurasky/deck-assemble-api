package com.deckassemble.cards.infrastructure.llm;

import com.deckassemble.cards.domain.BeginnerGuideContent;
import com.deckassemble.cards.domain.BeginnerGuideGenerator;
import com.deckassemble.cards.domain.BeginnerGuideSource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class RestClientBeginnerGuideGenerator implements BeginnerGuideGenerator {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    RestClientBeginnerGuideGenerator(
            BeginnerGuideLlmProperties properties, ObjectMapper objectMapper) {
        restClient =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .defaultHeader("User-Agent", properties.userAgent())
                        .requestFactory(requestFactory(properties))
                        .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public BeginnerGuideContent generate(BeginnerGuideSource source) {
        String payload =
                restClient
                        .post()
                        .uri("/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(new GenerationRequest(prompt(source)))
                        .retrieve()
                        .body(String.class);
        if (payload == null) {
            throw new RestClientException("Empty beginner guide response");
        }
        return parse(payload);
    }

    BeginnerGuideContent parse(String payload) {
        try {
            JsonNode content = objectMapper.readTree(payload);
            return new BeginnerGuideContent(
                    requiredText(content, "summary"),
                    requiredText(content, "examples"),
                    requiredText(content, "whenToUse"));
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new RestClientException("Invalid beginner guide response", exception);
        }
    }

    String prompt(BeginnerGuideSource source) {
        return ("Explain this Magic card to a beginner.%n"
                        + "Return JSON with non-empty string fields summary, examples, and whenToUse.%n"
                        + "Card: %s%nOracle text:%n%s%nRulings:%n%s%n")
                .formatted(
                        source.cardName(),
                        String.join(System.lineSeparator(), source.oracleTexts()),
                        String.join(System.lineSeparator(), source.rulings()));
    }

    private String requiredText(JsonNode content, String field) {
        JsonNode value = content.path(field);
        if (!value.isString() || value.asString().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value.asString();
    }

    private SimpleClientHttpRequestFactory requestFactory(BeginnerGuideLlmProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }

    private record GenerationRequest(String prompt) {}
}
