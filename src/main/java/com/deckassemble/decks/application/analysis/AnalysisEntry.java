package com.deckassemble.decks.application.analysis;

import com.deckassemble.cards.application.CardAnalysisView;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** One deck card joined with its catalog view, weighted by quantity. */
record AnalysisEntry(
        @Nullable Long deckCardId,
        Long printingId,
        int quantity,
        String ownershipStatus,
        CardAnalysisView card) {

    boolean isLand() {
        return allTypeLines().contains("land");
    }

    String allTypeLines() {
        StringBuilder types = new StringBuilder();
        appendLowercased(types, card.typeLine());
        card.faces().forEach(face -> appendLowercased(types, face.typeLine()));
        return types.toString();
    }

    String allOracleText() {
        StringBuilder text = new StringBuilder();
        card.faces().forEach(face -> appendLowercased(text, face.oracleText()));
        return text.toString();
    }

    Stream<String> manaCosts() {
        List<String> faceCosts =
                card.faces().stream()
                        .map(CardAnalysisView.Face::manaCost)
                        .filter(Objects::nonNull)
                        .toList();
        return faceCosts.isEmpty() ? Stream.ofNullable(card.manaCost()) : faceCosts.stream();
    }

    String primaryTypeLine() {
        if (card.typeLine() != null) {
            return card.typeLine();
        }
        return card.faces().stream()
                .map(CardAnalysisView.Face::typeLine)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private static void appendLowercased(StringBuilder target, @Nullable String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }
}
