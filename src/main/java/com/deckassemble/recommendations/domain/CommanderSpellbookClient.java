package com.deckassemble.recommendations.domain;

import java.util.List;

public interface CommanderSpellbookClient {

    List<SpellbookCombo> findCombos(String deckList);
}
