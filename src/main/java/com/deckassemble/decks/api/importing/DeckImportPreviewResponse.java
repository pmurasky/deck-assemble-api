package com.deckassemble.decks.api.importing;

import com.deckassemble.decks.application.importing.DeckImportService;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HTTP representation of a persisted deck import preview. */
public class DeckImportPreviewResponse {

    private final UUID token;
    private final Map<String, String> metadata;
    private final List<DeckImportService.ResolvedRow> resolvedRows;
    private final List<DeckImportService.AmbiguousRow> ambiguousRows;
    private final List<DeckImportService.UnmatchedRow> unmatchedRows;
    private final List<DeckImportService.InvalidRow> invalidRows;
    private final DeckImportService.Totals totals;

    public DeckImportPreviewResponse(DeckImportService.Preview preview) {
        token = preview.token();
        metadata = preview.metadata();
        resolvedRows = preview.rows().resolved();
        ambiguousRows = preview.rows().ambiguous();
        unmatchedRows = preview.rows().unmatched();
        invalidRows = preview.rows().invalid();
        totals = preview.totals();
    }

    public UUID getToken() {
        return token;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public List<DeckImportService.ResolvedRow> getResolvedRows() {
        return resolvedRows;
    }

    public List<DeckImportService.AmbiguousRow> getAmbiguousRows() {
        return ambiguousRows;
    }

    public List<DeckImportService.UnmatchedRow> getUnmatchedRows() {
        return unmatchedRows;
    }

    public List<DeckImportService.InvalidRow> getInvalidRows() {
        return invalidRows;
    }

    public DeckImportService.Totals getTotals() {
        return totals;
    }
}
