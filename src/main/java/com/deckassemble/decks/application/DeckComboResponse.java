package com.deckassemble.decks.application;

import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.util.List;

public record DeckComboResponse(boolean available, List<SpellbookCombo> combos) {}
