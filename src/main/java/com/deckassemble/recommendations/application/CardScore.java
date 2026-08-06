package com.deckassemble.recommendations.application;

import java.util.Set;
import org.jspecify.annotations.Nullable;

public record CardScore(@Nullable Double synergy, @Nullable Long inclusion, Set<String> cardlists) {

    public CardScore {
        cardlists = Set.copyOf(cardlists);
    }

    public CardScore(@Nullable Double synergy, @Nullable Long inclusion) {
        this(synergy, inclusion, Set.of());
    }
}
