package com.deckassemble.decks.application.importing;

import org.springframework.stereotype.Component;

@Component
public class MoxfieldCsvDeckImportParser implements DeckImportParser {

    @Override
    public String format() {
        return "MOXFIELD_CSV";
    }

    @Override
    public ParsedDeck parse(String source) {
        return GenericCsvDeckImportParser.parseCsv(
                source,
                format(),
                new GenericCsvDeckImportParser.CsvLayout("count", "edition", "board"));
    }
}
