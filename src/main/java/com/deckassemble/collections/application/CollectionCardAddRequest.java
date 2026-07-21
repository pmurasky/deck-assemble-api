package com.deckassemble.collections.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CollectionCardAddRequest(
        @NotNull Long cardPrintingId,
        @NotNull @Min(0) @Max(9999) Integer regularQuantity,
        @NotNull @Min(0) @Max(9999) Integer foilQuantity) {}
