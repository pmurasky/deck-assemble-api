package com.deckassemble.collections.api.physical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record StorageLocationRequest(
        @NotBlank @Size(max = 255) String name, @Nullable UUID parentId) {}
