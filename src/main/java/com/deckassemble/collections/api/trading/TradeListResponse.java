package com.deckassemble.collections.api.trading;

import com.deckassemble.collections.application.trading.TradeListService.TradeListItemView;
import com.deckassemble.collections.application.trading.TradeListService.TradeListView;
import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.trading.TradeListType;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record TradeListResponse(
        @Nullable Long id,
        Long profileId,
        String name,
        TradeListType type,
        TradeListVisibility visibility,
        List<TradeListItemResponse> items) {

    public static TradeListResponse from(TradeListView view) {
        return new TradeListResponse(
                view.id(),
                view.profileId(),
                view.name(),
                view.type(),
                view.visibility(),
                view.items().stream().map(TradeListItemResponse::from).toList());
    }

    public record TradeListItemResponse(
            @Nullable Long id,
            Long cardPrintingId,
            int quantity,
            @Nullable CardCondition condition,
            @Nullable PhysicalFinish finish,
            @Nullable String language) {

        static TradeListItemResponse from(TradeListItemView view) {
            return new TradeListItemResponse(
                    view.id(),
                    view.cardPrintingId(),
                    view.quantity(),
                    view.condition(),
                    view.finish(),
                    view.language());
        }
    }
}
