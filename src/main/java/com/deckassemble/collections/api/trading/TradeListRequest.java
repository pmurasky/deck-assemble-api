package com.deckassemble.collections.api.trading;

import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.trading.TradeListType;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record TradeListRequest(
        @NotBlank String name,
        @NotNull TradeListType type,
        @NotNull TradeListVisibility visibility,
        @Valid List<TradeListItemRequest> items) {

    public record TradeListItemRequest(
            @NotNull Long cardPrintingId,
            @Min(1) int quantity,
            @Nullable CardCondition condition,
            @Nullable PhysicalFinish finish,
            @Nullable String language) {}
}
