package com.deckassemble.decks.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.decks.domain.DeckCard;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Converts one supported external deck format into canonical import rows. */
public interface DeckImportParser {

    String format();

    ParsedDeck parse(String source);

    record ParsedDeck(Map<String, String> metadata, List<ParsedRow> rows) {}

    record ParsedRow(
            int lineNumber,
            int quantity,
            DeckCard.Section section,
            CardReference reference,
            @Nullable String error) {}
}
