package com.deckassemble.cards.domain;

import java.net.URI;

public interface ScryfallClient {

    CardSearchPage searchCards(String query);

    CardSearchPage searchCards(URI nextPageUri);
}
