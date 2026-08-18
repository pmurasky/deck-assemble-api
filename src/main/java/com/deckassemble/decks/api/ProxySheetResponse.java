package com.deckassemble.decks.api;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Print-layout data for the deck's unowned cards (proxy sheet). */
public record ProxySheetResponse(List<ProxySheetCard> cards) {

    public record ProxySheetCard(String name, @Nullable String imageUri, int quantity) {}
}
