package com.deckassemble.decks.api.publishing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Owner-supplied Markdown deck guide. No direct precedent in this codebase for a long-form-text
 * ceiling (existing {@code @Size} bounds like the 2000-char deck description are for short fields);
 * 20,000 characters is a generous but bounded ceiling for a "deck guide" writeup.
 */
public record DeckPrimerRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20_000) String markdownSource,
        @Nullable Integer expectedRevision) {}
