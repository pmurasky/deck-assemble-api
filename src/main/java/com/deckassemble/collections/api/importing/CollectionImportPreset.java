package com.deckassemble.collections.api.importing;

/** Supported collection CSV presets with their default column mappings. */
public enum CollectionImportPreset {
    DECKASSEMBLE,
    MOXFIELD,
    ARCHIDEKT,
    MANABOX,
    GENERIC;

    public CollectionColumnMapping defaultMapping() {
        return switch (this) {
            case DECKASSEMBLE, GENERIC ->
                    new CollectionColumnMapping(
                            "quantity", "name", "set", "collector_number", "scryfall_id");
            case MOXFIELD ->
                    new CollectionColumnMapping(
                            "count", "name", "edition", "collector number", null);
            case ARCHIDEKT ->
                    new CollectionColumnMapping(
                            "quantity", "name", "edition code", "collector number", null);
            case MANABOX ->
                    new CollectionColumnMapping(
                            "quantity", "name", "set code", "collector number", "scryfall id");
        };
    }
}
