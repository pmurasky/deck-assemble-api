package com.deckassemble.decks.api.importing;

import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.importing.DeckImportService;

public record DeckImportResultResponse(DeckResponse deck, int imported, int skipped) {

    public DeckImportResultResponse(DeckImportService.CommitResult result) {
        this(result.deck(), result.imported(), result.skipped());
    }
}
