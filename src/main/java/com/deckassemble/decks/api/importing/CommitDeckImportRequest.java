package com.deckassemble.decks.api.importing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CommitDeckImportRequest(
        @NotNull UUID previewToken,
        @NotBlank @Size(max = 200) String name,
        Set<@Positive Integer> excludedLineNumbers) {

    public CommitDeckImportRequest {
        excludedLineNumbers =
                excludedLineNumbers == null ? Set.of() : Set.copyOf(excludedLineNumbers);
    }
}
