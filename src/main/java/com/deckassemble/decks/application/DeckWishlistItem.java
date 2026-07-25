package com.deckassemble.decks.application;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record DeckWishlistItem(
        long deckCardId,
        long cardPrintingId,
        String cardName,
        int quantity,
        @Nullable BigDecimal unitPriceUsd,
        @Nullable BigDecimal lineTotalUsd) {}
