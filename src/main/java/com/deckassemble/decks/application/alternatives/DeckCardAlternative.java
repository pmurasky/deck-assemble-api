package com.deckassemble.decks.application.alternatives;

import com.deckassemble.recommendations.application.ScoreContribution;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A ranked alternative card with the reasons explaining its ranking. */
public record DeckCardAlternative(
        long printingId,
        String name,
        boolean owned,
        @Nullable BigDecimal priceUsd,
        BigDecimal total,
        List<ScoreContribution> contributions) {

    public DeckCardAlternative {
        contributions = List.copyOf(contributions);
    }
}
