package com.deckassemble.decks.application.analysis;

import com.deckassemble.cards.application.CardAnalysisView;
import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckComboService;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.DeckService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Composes deck composition, value, legality, and combo data into one analysis response. */
@Service
@Transactional
public class DeckAnalysisService {

    private final DeckService deckService;
    private final DeckCardService deckCardService;
    private final DeckComboService deckComboService;
    private final CardCatalogService cardCatalogService;
    private final CardPriceService cardPriceService;

    public DeckAnalysisService(
            DeckService deckService,
            DeckCardService deckCardService,
            DeckComboService deckComboService,
            CardCatalogService cardCatalogService,
            CardPriceService cardPriceService) {
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.deckComboService = deckComboService;
        this.cardCatalogService = cardCatalogService;
        this.cardPriceService = cardPriceService;
    }

    public DeckAnalysisResponse analyze(long deckId) {
        DeckLegalityResponse legality = deckService.legality(deckId);
        List<DeckCardResponse> cards = deckCardService.listCards(deckId);
        Map<Long, CardAnalysisView> views =
                cardCatalogService.getAnalysisViewsByPrintingIds(cardPrintingIds(cards));
        List<AnalysisEntry> entries = entries(cards, views);
        Map<Long, CardPrice> prices = cardPriceService.latestPrices(entryPrintingIds(entries));
        return DeckAnalysisResponse.from(
                entries, prices, legality, deckComboService.getCombos(deckId));
    }

    private static List<AnalysisEntry> entries(
            List<DeckCardResponse> cards, Map<Long, CardAnalysisView> views) {
        return cards.stream()
                .filter(DeckAnalysisService::includedInAnalysis)
                .flatMap(card -> entry(card, views).stream())
                .toList();
    }

    // ponytail: analysis covers the playable deck (commander + main); sideboard/companion/maybe
    // excluded like most deck-stat tools. Add a section query param if users ask for them.
    private static boolean includedInAnalysis(DeckCardResponse card) {
        return "COMMANDER".equals(card.deckSection()) || "MAIN_DECK".equals(card.deckSection());
    }

    private static Optional<AnalysisEntry> entry(
            DeckCardResponse card, Map<Long, CardAnalysisView> views) {
        CardAnalysisView view = views.get(card.cardPrintingId());
        return view == null
                ? Optional.empty()
                : Optional.of(
                        new AnalysisEntry(
                                card.cardPrintingId(),
                                card.quantity(),
                                card.ownershipStatus(),
                                view));
    }

    private static List<Long> cardPrintingIds(List<DeckCardResponse> cards) {
        return cards.stream().map(DeckCardResponse::cardPrintingId).toList();
    }

    private static List<Long> entryPrintingIds(List<AnalysisEntry> entries) {
        return entries.stream().map(AnalysisEntry::printingId).toList();
    }
}
