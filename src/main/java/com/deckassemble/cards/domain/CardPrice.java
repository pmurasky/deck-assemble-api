package com.deckassemble.cards.domain;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record CardPrice(
        @Nullable BigDecimal usd,
        @Nullable BigDecimal usdFoil,
        @Nullable BigDecimal eur,
        @Nullable BigDecimal tix) {}
