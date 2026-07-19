package com.deckassemble.cards.infrastructure.scryfall;

import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallBulkData;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallList;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallSet;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

public interface ScryfallClient {

  List<ScryfallSet> getSets();

  ScryfallList<ScryfallCard> searchCards(String query);

  ScryfallBulkData getBulkData(String type);

  InputStream download(URI uri);
}
