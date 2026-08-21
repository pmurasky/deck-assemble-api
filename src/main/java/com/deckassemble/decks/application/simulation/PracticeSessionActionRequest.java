package com.deckassemble.decks.application.simulation;

import jakarta.validation.constraints.NotNull;

public record PracticeSessionActionRequest(@NotNull Long printingId) {}
