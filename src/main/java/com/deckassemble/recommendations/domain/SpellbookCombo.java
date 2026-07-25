package com.deckassemble.recommendations.domain;

import java.util.List;

public record SpellbookCombo(
        String id,
        List<String> cards,
        List<String> produces,
        String description,
        String prerequisites) {}
