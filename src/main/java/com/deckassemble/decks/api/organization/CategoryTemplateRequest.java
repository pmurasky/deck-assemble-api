package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Body for creating a template or replacing its name/items; item order is list order. */
public record CategoryTemplateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotEmpty List<@NotBlank @Size(max = 100) String> itemNames) {}
