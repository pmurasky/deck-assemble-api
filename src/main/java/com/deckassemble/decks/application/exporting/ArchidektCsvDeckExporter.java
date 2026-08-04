package com.deckassemble.decks.application.exporting;

import com.deckassemble.decks.domain.DeckCard;
import org.springframework.stereotype.Component;

@Component
public class ArchidektCsvDeckExporter implements DeckExporter {

    @Override
    public DeckExportFormat format() {
        return DeckExportFormat.ARCHIDEKT_CSV;
    }

    @Override
    public String header() {
        return "Quantity,Name,Edition Code,Collector Number,Categories,Scryfall ID\n";
    }

    @Override
    public String line(ExportCard card) {
        return "%d,%s,%s,%s,%s,%s"
                .formatted(
                        card.quantity(),
                        DeckExporter.csv(card.displayName()),
                        DeckExporter.csv(card.setCode()),
                        DeckExporter.csv(card.collectorNumber()),
                        category(card.section()),
                        card.scryfallId());
    }

    private static String category(DeckCard.Section section) {
        return switch (section) {
            case COMMANDER -> "Commander";
            case MAIN_DECK -> "Mainboard";
            case SIDEBOARD -> "Sideboard";
            case COMPANION -> "Companion";
            case MAYBE_BOARD -> "Maybeboard";
        };
    }
}
