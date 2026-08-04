package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.recommendations.domain.CommanderSpellbookClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds known combos for a deck via the Commander Spellbook. */
@Service
@Transactional(readOnly = true)
public class DeckComboService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckComboService.class);

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;
    private final CardCatalogService cardCatalogService;
    private final CommanderSpellbookClient commanderSpellbookClient;

    public DeckComboService(
            DeckAccessGuard deckAccessGuard,
            DeckCardRepository deckCardRepository,
            CardCatalogService cardCatalogService,
            CommanderSpellbookClient commanderSpellbookClient) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
        this.cardCatalogService = cardCatalogService;
        this.commanderSpellbookClient = commanderSpellbookClient;
    }

    public DeckComboResponse getCombos(long deckId) {
        deckAccessGuard.owned(deckId);
        var cards = deckCardRepository.findByDeckId(deckId);
        var names = cardCatalogService.getCardsByPrintingIds(cardPrintingIds(cards));
        var deckList = withNames(cards, names);
        if (deckList.isBlank()) {
            return new DeckComboResponse(true, List.of());
        }
        try {
            return new DeckComboResponse(true, commanderSpellbookClient.findCombos(deckList));
        } catch (org.springframework.web.client.RestClientException exception) {
            LOGGER.warn("Commander Spellbook lookup failed for deck {}", deckId, exception);
            return new DeckComboResponse(false, List.of());
        }
    }

    private static List<Long> cardPrintingIds(List<DeckCard> cards) {
        return cards.stream()
                .filter(DeckComboService::includedInSpellbookLookup)
                .map(DeckCard::getCardPrintingId)
                .toList();
    }

    private static String withNames(List<DeckCard> cards, Map<Long, Card> cardsByPrintingId) {
        return cards.stream()
                .filter(DeckComboService::includedInSpellbookLookup)
                .flatMap(card -> deckListLine(card, cardsByPrintingId).stream())
                .collect(Collectors.joining("\n"));
    }

    private static boolean includedInSpellbookLookup(DeckCard card) {
        return card.getDeckSection() == DeckCard.Section.COMMANDER
                || card.getDeckSection() == DeckCard.Section.MAIN_DECK
                || card.getDeckSection() == DeckCard.Section.COMPANION;
    }

    private static Optional<String> deckListLine(DeckCard card, Map<Long, Card> cardsByPrintingId) {
        Card catalogCard = cardsByPrintingId.get(card.getCardPrintingId());
        return catalogCard == null
                ? Optional.empty()
                : Optional.of(card.getQuantity() + " " + catalogCard.getName());
    }
}
