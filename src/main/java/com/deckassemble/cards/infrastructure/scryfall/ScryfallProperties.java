package com.deckassemble.cards.infrastructure.scryfall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deckassemble.scryfall")
public record ScryfallProperties(
    @NotBlank String baseUrl,
    @NotBlank String userAgent,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @NotNull Duration requestDelay) {

  public ScryfallProperties {
    baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
