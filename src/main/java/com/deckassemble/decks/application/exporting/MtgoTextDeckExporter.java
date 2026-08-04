package com.deckassemble.decks.application.exporting;

import com.deckassemble.decks.domain.DeckCard;
import org.springframework.stereotype.Component;

@Component
public class MtgoTextDeckExporter implements DeckExporter {

    @Override
    public DeckExportFormat format() {
        return DeckExportFormat.MTGO_TEXT;
    }

    @Override
    public String line(ExportCard card) {
        return "%d %s [%s:%s]"
                .formatted(
                        card.quantity(),
                        card.displayName(),
                        card.setCode(),
                        card.collectorNumber());
    }

    @Override
    public String heading(DeckCard.Section section) {
        return switch (section) {
            case COMMANDER -> "Commander";
            case MAIN_DECK -> "Main Deck";
            case SIDEBOARD -> "Sideboard";
            case COMPANION -> "Companion";
            case MAYBE_BOARD -> "Maybeboard";
        };
    }
}
