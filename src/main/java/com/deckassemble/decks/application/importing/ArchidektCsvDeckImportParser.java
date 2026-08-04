package com.deckassemble.decks.application.importing;

import org.springframework.stereotype.Component;

@Component
public class ArchidektCsvDeckImportParser implements DeckImportParser {

    @Override
    public String format() {
        return "ARCHIDEKT_CSV";
    }

    @Override
    public ParsedDeck parse(String source) {
        return GenericCsvDeckImportParser.parseCsv(
                source,
                format(),
                new GenericCsvDeckImportParser.CsvLayout("quantity", "edition code", "categories"));
    }
}
