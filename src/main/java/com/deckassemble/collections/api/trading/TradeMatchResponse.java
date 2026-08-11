package com.deckassemble.collections.api.trading;

import com.deckassemble.collections.application.trading.TradeMatchService.TradeMatchItemView;
import com.deckassemble.collections.application.trading.TradeMatchService.TradeMatchView;
import com.deckassemble.collections.application.trading.TradeMatchService.ValueDeltaView;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record TradeMatchResponse(
        long leftListId,
        long rightListId,
        List<TradeMatchItemResponse> matches,
        List<ValueDeltaResponse> valueDeltas,
        int unpricedItems) {

    public static TradeMatchResponse from(TradeMatchView view) {
        return new TradeMatchResponse(
                view.leftListId(),
                view.rightListId(),
                view.matches().stream().map(TradeMatchItemResponse::from).toList(),
                view.valueDeltas().stream().map(ValueDeltaResponse::from).toList(),
                view.unpricedItems());
    }

    public record TradeMatchItemResponse(
            Long fromListId,
            Long toListId,
            @Nullable Long fromItemId,
            @Nullable Long toItemId,
            Long requestedCardPrintingId,
            Long matchedCollectionCardId,
            Long matchedCollectionCardPrintingId,
            int quantity,
            int availableQuantity,
            boolean exactPrinting) {

        static TradeMatchItemResponse from(TradeMatchItemView view) {
            return new TradeMatchItemResponse(
                    view.fromListId(),
                    view.toListId(),
                    view.fromItemId(),
                    view.toItemId(),
                    view.requestedCardPrintingId(),
                    view.matchedCollectionCardId(),
                    view.matchedCollectionCardPrintingId(),
                    view.quantity(),
                    view.availableQuantity(),
                    view.exactPrinting());
        }
    }

    public record ValueDeltaResponse(
            String currency, BigDecimal leftToRight, BigDecimal rightToLeft) {
        static ValueDeltaResponse from(ValueDeltaView view) {
            return new ValueDeltaResponse(view.currency(), view.leftToRight(), view.rightToLeft());
        }
    }
}
