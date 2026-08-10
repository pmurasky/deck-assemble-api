package com.deckassemble.cards.domain;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public interface ScryfallClient {

    CardSearchPage searchCards(String query);

    CardSearchPage searchCards(URI nextPageUri);

    CardPrice getCardPrice(String scryfallCardId);

    /** Fetches Tagger oracle tags inverted into a per-oracle-id index of tag labels. */
    Map<String, Set<String>> fetchOracleTagAssignments();
}
