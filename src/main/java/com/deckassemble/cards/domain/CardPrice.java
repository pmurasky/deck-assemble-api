package com.deckassemble.cards.domain;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record CardPrice(
        @Nullable BigDecimal usd,
        @Nullable BigDecimal usdFoil,
        @Nullable BigDecimal eur,
        @Nullable BigDecimal tix) {

    /**
     * Resolves the amount for an allow-listed currency code, defaulting to {@link #usd()} for any
     * unrecognized value. Mirrors the currency convention used by upgrade-plan pricing.
     */
    public @Nullable BigDecimal forCurrency(@Nullable String currency) {
        return switch (currency == null ? "usd" : currency) {
            case "usdFoil" -> usdFoil;
            case "eur" -> eur;
            case "tix" -> tix;
            default -> usd;
        };
    }
}
