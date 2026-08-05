package com.deckassemble.decks.application.exporting;

import com.deckassemble.decks.domain.DeckCard;
import org.springframework.stereotype.Component;

@Component
public class GenericCsvDeckExporter implements DeckExporter {

    @Override
    public DeckExportFormat format() {
        return DeckExportFormat.GENERIC_CSV;
    }

    @Override
    public String header() {
        return "quantity,name,set,collector_number,section,scryfall_id\n";
    }

    @Override
    public String line(ExportCard card) {
        return "%d,%s,%s,%s,%s,%s"
                .formatted(
                        card.quantity(),
                        DeckExporter.csv(card.displayName()),
                        DeckExporter.csv(card.printing().setCode()),
                        DeckExporter.csv(card.printing().collectorNumber()),
                        section(card.section()),
                        card.printing().scryfallId());
    }

    private static String section(DeckCard.Section section) {
        return switch (section) {
            case COMMANDER -> "commander";
            case MAIN_DECK -> "main";
            case SIDEBOARD -> "sideboard";
            case COMPANION -> "companion";
            case MAYBE_BOARD -> "maybeboard";
        };
    }
}
