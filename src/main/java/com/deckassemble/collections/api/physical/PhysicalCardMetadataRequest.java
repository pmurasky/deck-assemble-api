package com.deckassemble.collections.api.physical;

import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PhysicalCardMetadataRequest(
        @Nullable CardCondition condition,
        @Nullable @Size(min = 2, max = 10) String language,
        @Nullable PhysicalFinish finish,
        @Nullable @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal purchasePrice,
        @Nullable @Pattern(regexp = "^[A-Za-z]{3}$") String purchaseCurrency,
        @Nullable LocalDate purchaseDate,
        @Nullable @Size(max = 2000) String notes,
        @Nullable UUID storageLocationId) {}
