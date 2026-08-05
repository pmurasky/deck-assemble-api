package com.deckassemble.decks.application.exporting;

import com.deckassemble.decks.domain.DeckCard;
import org.springframework.stereotype.Component;

@Component
public class ArenaTextDeckExporter implements DeckExporter {

    @Override
    public DeckExportFormat format() {
        return DeckExportFormat.ARENA_TEXT;
    }

    @Override
    public String line(ExportCard card) {
        return "%d %s (%s) %s"
                .formatted(
                        card.quantity(),
                        card.displayName(),
                        card.printing().setCode(),
                        card.printing().collectorNumber());
    }

    @Override
    public String heading(DeckCard.Section section) {
        return switch (section) {
            case COMMANDER -> "Commander";
            case MAIN_DECK -> "Deck";
            case SIDEBOARD -> "Sideboard";
            case COMPANION -> "Companion";
            case MAYBE_BOARD -> "Maybeboard";
        };
    }
}
