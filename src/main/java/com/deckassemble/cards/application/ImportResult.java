package com.deckassemble.cards.application;

public record ImportResult(long runId, int recordsRead, int recordsCreated, int recordsUpdated,
    int recordsFailed) {
}
