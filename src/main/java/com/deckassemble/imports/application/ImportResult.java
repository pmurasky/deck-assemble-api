package com.deckassemble.imports.application;

public record ImportResult(
        long runId, int recordsRead, int recordsCreated, int recordsUpdated, int recordsFailed) {}
