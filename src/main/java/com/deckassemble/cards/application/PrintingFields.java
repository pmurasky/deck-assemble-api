package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardPrinting;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

final class PrintingFields {

    private PrintingFields() {}

    static <T> @Nullable T of(@Nullable CardPrinting printing, Function<CardPrinting, T> getter) {
        return printing == null ? null : getter.apply(printing);
    }

    static <T> T of(@Nullable CardPrinting printing, Function<CardPrinting, T> getter, T fallback) {
        return printing == null ? fallback : getter.apply(printing);
    }
}
