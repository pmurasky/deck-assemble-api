package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.cards.infrastructure.MagicSetRepository;
import com.deckassemble.cards.infrastructure.scryfall.ScryfallClient;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallImageUris;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardImportService {

  private final ScryfallClient scryfallClient;
  private final CardRepository cardRepository;
  private final MagicSetRepository magicSetRepository;
  private final CardPrintingRepository cardPrintingRepository;

  public CardImportService(ScryfallClient scryfallClient, CardRepository cardRepository,
      MagicSetRepository magicSetRepository, CardPrintingRepository cardPrintingRepository) {
    this.scryfallClient = scryfallClient;
    this.cardRepository = cardRepository;
    this.magicSetRepository = magicSetRepository;
    this.cardPrintingRepository = cardPrintingRepository;
  }

  @Transactional
  public int importQuery(String query) {
    var page = scryfallClient.searchCards(query);
    int importedCount = importPage(page.data());
    while (page.hasMore()) {
      page = scryfallClient.searchCards(nextPage(page.nextPage()));
      importedCount += importPage(page.data());
    }
    return importedCount;
  }

  private int importPage(List<ScryfallCard> cards) {
    cards.forEach(this::importCard);
    return cards.size();
  }

  private URI nextPage(URI nextPage) {
    if (nextPage == null) {
      throw new IllegalStateException("Scryfall response marked additional pages without a next page URL");
    }
    return nextPage;
  }

  private void importCard(ScryfallCard source) {
    if (source.id() == null || source.oracleId() == null || source.setId() == null || source.set() == null) {
      return;
    }
    Card card = cardRepository.findByScryfallOracleId(source.oracleId())
        .orElseGet(() -> new Card(source.oracleId(), source.name()));
    applyCardDetails(card, source);
    card = cardRepository.save(card);
    MagicSet set = magicSetRepository.findBySetCode(source.set())
        .orElseGet(() -> magicSetRepository.save(
            new MagicSet(source.setId(), source.set(), source.setName())));
    savePrinting(card, set, source);
  }

  private void applyCardDetails(Card card, ScryfallCard source) {
    card.setManaCost(source.manaCost());
    card.setManaValue(source.cmc() == null ? null : BigDecimal.valueOf(source.cmc()));
    card.setTypeLine(source.typeLine());
    card.setOracleText(source.oracleText());
    card.setPower(source.power());
    card.setToughness(source.toughness());
    card.setLoyalty(source.loyalty());
    card.setColors(join(source.colors()));
    card.setColorIdentity(join(source.colorIdentity()));
    card.setKeywords(join(source.keywords()));
    card.setLayout(source.layout());
    card.setReserved(source.reserved());
  }

  private String join(List<String> values) {
    return values == null ? null : String.join(",", values);
  }

  private void savePrinting(Card card, MagicSet set, ScryfallCard source) {
    CardPrinting printing = cardPrintingRepository.findByScryfallCardId(source.id())
        .orElseGet(() -> new CardPrinting(card, set, source.id()));
    printing.setCollectorNumber(source.collectorNumber());
    printing.setRarity(source.rarity());
    printing.setArtist(source.artist());
    printing.setFlavorText(source.flavorText());
    printing.setReleasedAt(source.releasedAt());
    printing.setFoilAvailable(source.foil());
    printing.setNonfoilAvailable(source.nonfoil());
    printing.setPromo(source.promo());
    printing.setDigital(source.digital());
    printing.setLanguage(source.lang());
    ScryfallImageUris imageUris = imageUris(source);
    if (imageUris != null) {
      printing.setImageUriSmall(imageUris.small());
      printing.setImageUriNormal(imageUris.normal());
      printing.setImageUriLarge(imageUris.large());
    }
    cardPrintingRepository.save(printing);
  }

  private ScryfallImageUris imageUris(ScryfallCard source) {
    if (source.imageUris() != null) {
      return source.imageUris();
    }
    if (source.cardFaces() == null) {
      return null;
    }
    return source.cardFaces().stream().map(face -> face.imageUris()).filter(uri -> uri != null)
        .findFirst().orElse(null);
  }
}
