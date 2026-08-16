package com.deckassemble.cards.infrastructure.llm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/// Connection settings for the beginner-guide LLM endpoint.
@Validated
@ConfigurationProperties(prefix = "deckassemble.beginner-guide-llm")
public record BeginnerGuideLlmProperties(
        @NotBlank String baseUrl,
        @NotBlank String userAgent,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {

    public BeginnerGuideLlmProperties {
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
