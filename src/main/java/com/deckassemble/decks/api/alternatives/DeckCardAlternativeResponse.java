package com.deckassemble.decks.api.alternatives;

import com.deckassemble.decks.application.alternatives.DeckCardAlternative;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A ranked alternative for a deck card, with the reasons for its ranking. */
public record DeckCardAlternativeResponse(
        long cardPrintingId,
        String name,
        boolean owned,
        @Nullable BigDecimal priceUsd,
        BigDecimal total,
        List<DeckCardAlternativeReason> reasons) {

    public static DeckCardAlternativeResponse from(DeckCardAlternative alternative) {
        return new DeckCardAlternativeResponse(
                alternative.printingId(),
                alternative.name(),
                alternative.owned(),
                alternative.priceUsd(),
                alternative.total(),
                alternative.contributions().stream().map(DeckCardAlternativeReason::from).toList());
    }
}
