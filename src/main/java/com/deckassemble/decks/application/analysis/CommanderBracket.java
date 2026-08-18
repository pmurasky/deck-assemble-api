package com.deckassemble.decks.application.analysis;

import java.util.List;

/** Commander Bracket assessment: 1-5 level plus the cards that drove the score. */
public record CommanderBracket(int level, List<String> flaggedCards) {}
