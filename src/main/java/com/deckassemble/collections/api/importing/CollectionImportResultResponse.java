package com.deckassemble.collections.api.importing;

import com.deckassemble.collections.application.CollectionResponse;
import com.deckassemble.collections.application.importing.CollectionImportService;

public record CollectionImportResultResponse(
        CollectionResponse collection, int imported, int skipped) {

    public CollectionImportResultResponse(CollectionImportService.CommitResult result) {
        this(result.collection(), result.imported(), result.skipped());
    }
}
