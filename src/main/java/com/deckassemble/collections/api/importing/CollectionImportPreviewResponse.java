package com.deckassemble.collections.api.importing;

import com.deckassemble.collections.application.importing.CollectionImportService;
import java.util.List;
import java.util.UUID;

/** HTTP representation of a persisted collection import preview. */
public record CollectionImportPreviewResponse(
        UUID token,
        List<CollectionImportService.ResolvedRow> resolvedRows,
        List<CollectionImportService.AmbiguousRow> ambiguousRows,
        List<CollectionImportService.UnmatchedRow> unmatchedRows,
        List<CollectionImportService.InvalidRow> invalidRows,
        CollectionImportService.Totals totals) {

    public CollectionImportPreviewResponse(CollectionImportService.Preview preview) {
        this(
                preview.token(),
                preview.rows().resolved(),
                preview.rows().ambiguous(),
                preview.rows().unmatched(),
                preview.rows().invalid(),
                preview.totals());
    }
}
