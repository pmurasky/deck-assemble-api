package com.deckassemble.decks.application.importing;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MtgoTextDeckImportParser implements DeckImportParser {

    private static final Pattern ROW =
            Pattern.compile(
                    "^(?<quantity>\\d+)\\s+(?<name>.+)\\s+\\[(?<set>[^]:]+):(?<collector>[^]]+)]$");

    @Override
    public String format() {
        return "MTGO_TEXT";
    }

    @Override
    public ParsedDeck parse(String source) {
        return DeckAssembleTextDeckImportParser.parseText(source, format(), ROW);
    }
}
