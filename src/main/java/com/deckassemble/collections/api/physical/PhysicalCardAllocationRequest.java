package com.deckassemble.collections.api.physical;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.Nullable;

public record PhysicalCardAllocationRequest(
        @Nullable Long deckCardId,
        @Nullable Long collectionCardId,
        @Nullable @Min(1) @Max(9999) Integer quantity) {}
