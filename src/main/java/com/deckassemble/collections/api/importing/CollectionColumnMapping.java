package com.deckassemble.collections.api.importing;

import com.deckassemble.collections.application.importing.CollectionCsvParser;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Column header names used to read a collection CSV; unset fields fall back to the preset. */
public record CollectionColumnMapping(
        @Nullable String quantityColumn,
        @Nullable String nameColumn,
        @Nullable String setCodeColumn,
        @Nullable String collectorNumberColumn,
        @Nullable String scryfallIdColumn) {

    public CollectionColumnMapping merge(@Nullable CollectionColumnMapping overrides) {
        if (overrides == null) {
            return this;
        }
        return new CollectionColumnMapping(
                firstNonNull(overrides.quantityColumn(), quantityColumn),
                firstNonNull(overrides.nameColumn(), nameColumn),
                firstNonNull(overrides.setCodeColumn(), setCodeColumn),
                firstNonNull(overrides.collectorNumberColumn(), collectorNumberColumn),
                firstNonNull(overrides.scryfallIdColumn(), scryfallIdColumn));
    }

    public CollectionCsvParser.ColumnLayout toLayout() {
        if (quantityColumn == null || nameColumn == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Column mapping requires quantity and name columns");
        }
        return new CollectionCsvParser.ColumnLayout(
                quantityColumn, nameColumn, setCodeColumn, collectorNumberColumn, scryfallIdColumn);
    }

    private static @Nullable String firstNonNull(@Nullable String first, @Nullable String second) {
        return first != null ? first : second;
    }
}
