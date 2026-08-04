package com.deckassemble.decks.application.exporting;

import com.deckassemble.decks.domain.DeckCard;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Converts canonical deck rows into one deterministic external format. */
public interface DeckExporter {

    DeckExportFormat format();

    String line(ExportCard card);

    default String header() {
        return "";
    }

    default @Nullable String heading(DeckCard.Section section) {
        return null;
    }

    default String export(List<ExportCard> cards) {
        StringBuilder output = new StringBuilder(header());
        DeckCard.Section previous = null;
        for (ExportCard card : sorted(cards)) {
            String heading = heading(card.section());
            if (heading != null && card.section() != previous) {
                if (previous != null) {
                    output.append('\n');
                }
                output.append(heading).append('\n');
            }
            output.append(line(card)).append('\n');
            previous = card.section();
        }
        return output.toString();
    }

    private static List<ExportCard> sorted(List<ExportCard> cards) {
        return cards.stream()
                .sorted(
                        Comparator.comparingInt((ExportCard card) -> card.section().ordinal())
                                .thenComparing(
                                        ExportCard::displayName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(ExportCard::setCode)
                                .thenComparing(ExportCard::collectorNumber)
                                .thenComparing(ExportCard::scryfallId))
                .toList();
    }

    static String csv(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    record ExportCard(
            DeckCard.Section section,
            int quantity,
            String name,
            @Nullable String flavorName,
            String setCode,
            String collectorNumber,
            String scryfallId) {

        public String displayName() {
            return flavorName == null || flavorName.isBlank() ? name : flavorName;
        }
    }
}
