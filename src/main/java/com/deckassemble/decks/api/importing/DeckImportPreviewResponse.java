package com.deckassemble.decks.api.importing;

import com.deckassemble.decks.application.importing.DeckImportService;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HTTP representation of a persisted deck import preview. */
public record DeckImportPreviewResponse(
        UUID token,
        Map<String, String> metadata,
        List<DeckImportService.ResolvedRow> resolvedRows,
        List<DeckImportService.AmbiguousRow> ambiguousRows,
        List<DeckImportService.UnmatchedRow> unmatchedRows,
        List<DeckImportService.InvalidRow> invalidRows,
        DeckImportService.Totals totals) {

    public DeckImportPreviewResponse(DeckImportService.Preview preview) {
        this(
                preview.token(),
                preview.metadata(),
                preview.rows().resolved(),
                preview.rows().ambiguous(),
                preview.rows().unmatched(),
                preview.rows().invalid(),
                preview.totals());
    }
}
