package com.deckassemble.decks.application.exporting;

/** Supported external deck export formats. */
public enum DeckExportFormat {
    DECKASSEMBLE_TEXT("text/plain", "deckassemble.txt"),
    GENERIC_CSV("text/csv", "generic.csv"),
    MOXFIELD_CSV("text/csv", "moxfield.csv"),
    ARCHIDEKT_CSV("text/csv", "archidekt.csv"),
    ARENA_TEXT("text/plain", "arena.txt"),
    MTGO_TEXT("text/plain", "mtgo.txt");

    private final String mediaType;
    private final String filenameSuffix;

    DeckExportFormat(String mediaType, String filenameSuffix) {
        this.mediaType = mediaType;
        this.filenameSuffix = filenameSuffix;
    }

    public String mediaType() {
        return mediaType;
    }

    public String filenameSuffix() {
        return filenameSuffix;
    }
}
