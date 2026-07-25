package com.deckassemble.recommendations.domain;

public interface EdhrecClient {

    String fetchCommanderData(String commanderSlug);

    String fetchTopCommanders();
}
