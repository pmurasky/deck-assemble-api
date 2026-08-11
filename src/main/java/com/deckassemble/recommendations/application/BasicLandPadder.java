package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.ManaPips;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Pads a drafted main deck to its target size with basic lands, weighted by the deck's colored pip
 * demand, recording a gap when no basics are available in the catalog.
 */
@Service
public class BasicLandPadder {

    private static final List<String> COLOR_ORDER = List.of("W", "U", "B", "R", "G");
    private static final Map<String, String> COLOR_TO_BASIC =
            Map.of(
                    "W", "Plains",
                    "U", "Island",
                    "B", "Swamp",
                    "R", "Mountain",
                    "G", "Forest");

    private final CardCatalogService cardCatalogService;

    public BasicLandPadder(CardCatalogService cardCatalogService) {
        this.cardCatalogService = cardCatalogService;
    }

    public List<DeckCandidate> pad(
            List<DeckCandidate> picked, Set<String> identity, int targetSize, List<String> gaps) {
        var cards = new ArrayList<>(picked);
        var missing = targetSize - picked.size();
        if (missing == 0) {
            return cards;
        }
        var basics = basicLands(identity);
        if (basics.isEmpty()) {
            gaps.add(missing + " slots could not be filled from your collection");
            return cards;
        }
        var available =
                COLOR_ORDER.stream()
                        .filter(color -> basics.containsKey(COLOR_TO_BASIC.get(color)))
                        .collect(Collectors.toSet());
        var allocation = BasicLandAllocation.byPips(available, deckPips(picked), missing);
        appendAllocatedBasics(cards, basics, allocation);
        return cards;
    }

    private void appendAllocatedBasics(
            List<DeckCandidate> cards,
            Map<String, DeckCandidate> basics,
            Map<String, Integer> allocation) {
        allocation.forEach(
                (color, count) -> {
                    var basic = basics.get(COLOR_TO_BASIC.get(color));
                    for (var i = 0; i < count; i++) {
                        cards.add(basic);
                    }
                });
    }

    private ManaPips deckPips(List<DeckCandidate> picked) {
        var total = ManaPips.ZERO;
        for (var candidate : picked) {
            total = total.plus(ManaPips.fromManaCost(candidate.card().getManaCost()));
        }
        return total;
    }

    private Map<String, DeckCandidate> basicLands(Set<String> identity) {
        var names =
                COLOR_ORDER.stream().filter(identity::contains).map(COLOR_TO_BASIC::get).toList();
        var cardsByName = new LinkedHashMap<String, Card>();
        cardCatalogService
                .getCardsByNames(names)
                .forEach(card -> cardsByName.put(card.getName(), card));
        var printingIds =
                cardCatalogService.getLatestPrintingIdByCardIds(
                        cardsByName.values().stream().map(Card::getId).toList());
        var basics = new LinkedHashMap<String, DeckCandidate>();
        for (var name : names) {
            var card = cardsByName.get(name);
            var printingId = card != null ? printingIds.get(card.getId()) : null;
            if (card != null && printingId != null) {
                basics.put(name, new DeckCandidate(printingId, card, Category.LAND, null));
            }
        }
        return basics;
    }
}
