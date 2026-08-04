package com.deckassemble.decks.application.exporting;

import com.deckassemble.decks.domain.DeckCard;
import org.springframework.stereotype.Component;

@Component
public class MoxfieldCsvDeckExporter implements DeckExporter {

    @Override
    public DeckExportFormat format() {
        return DeckExportFormat.MOXFIELD_CSV;
    }

    @Override
    public String header() {
        return "Count,Name,Edition,Collector Number,Board,Scryfall ID\n";
    }

    @Override
    public String line(ExportCard card) {
        return "%d,%s,%s,%s,%s,%s"
                .formatted(
                        card.quantity(),
                        DeckExporter.csv(card.displayName()),
                        DeckExporter.csv(card.setCode()),
                        DeckExporter.csv(card.collectorNumber()),
                        board(card.section()),
                        card.scryfallId());
    }

    private static String board(DeckCard.Section section) {
        return switch (section) {
            case COMMANDER -> "commanders";
            case MAIN_DECK -> "mainboard";
            case SIDEBOARD -> "sideboard";
            case COMPANION -> "companions";
            case MAYBE_BOARD -> "maybeboard";
        };
    }
}
