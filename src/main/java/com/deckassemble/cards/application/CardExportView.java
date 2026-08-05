package com.deckassemble.cards.application;

import org.jspecify.annotations.Nullable;

/** Immutable card-printing data exposed to deck export consumers. */
public record CardExportView(
        Long printingId,
        String canonicalName,
        @Nullable String flavorName,
        PrintingReference printing) {

    public String displayName() {
        return flavorName == null || flavorName.isBlank() ? canonicalName : flavorName;
    }

    public record PrintingReference(String setCode, String collectorNumber, String scryfallId) {}
}
