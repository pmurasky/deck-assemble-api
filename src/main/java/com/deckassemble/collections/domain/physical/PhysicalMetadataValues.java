package com.deckassemble.collections.domain.physical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PhysicalMetadataValues(
        @Nullable CardCondition condition,
        @Nullable String language,
        @Nullable PhysicalFinish finish,
        @Nullable BigDecimal purchasePrice,
        @Nullable String purchaseCurrency,
        @Nullable LocalDate purchaseDate,
        @Nullable String notes,
        @Nullable UUID storageLocationId) {}
