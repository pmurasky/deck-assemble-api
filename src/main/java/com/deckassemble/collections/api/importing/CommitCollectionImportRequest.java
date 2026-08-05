package com.deckassemble.collections.api.importing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CommitCollectionImportRequest(
        @NotNull UUID previewToken,
        @NotBlank @Size(max = 255) String name,
        Set<@Positive Integer> excludedLineNumbers) {

    public CommitCollectionImportRequest {
        excludedLineNumbers =
                excludedLineNumbers == null ? Set.of() : Set.copyOf(excludedLineNumbers);
    }
}
