package com.deckassemble.recommendations.application;

import org.jspecify.annotations.Nullable;

public record RefreshOutcome(boolean success, int cardsUpdated, @Nullable String errorSummary) {

    public static RefreshOutcome completed(int cardsUpdated) {
        return new RefreshOutcome(true, cardsUpdated, null);
    }

    public static RefreshOutcome failed(String errorSummary) {
        return new RefreshOutcome(false, 0, errorSummary);
    }
}
