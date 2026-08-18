package com.deckassemble.decks.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCard.Section;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Base evaluator for 60-card constructed formats: minimum deck size, four-copy limit for
 * non-basic cards, and per-card legality status for the format.
 *
 * @since 1.0
 */
abstract class ConstructedLegalityEvaluator implements FormatLegalityEvaluator {

    private static final int MINIMUM_DECK_SIZE = 60;
    private static final int MAX_COPIES = 4;

    private final CardPrintingRepository cardPrintingRepository;

    ConstructedLegalityEvaluator(CardPrintingRepository cardPrintingRepository) {
        this.cardPrintingRepository = cardPrintingRepository;
    }

    @Override
    public DeckLegalityResponse evaluate(Deck deck, List<DeckCard> deckCards) {
        List<DeckLegalityResponse.Violation> violations = new ArrayList<>();
        List<CardPrinting> printings = expandMainDeck(deckCards, violations);
        validateDeckSize(printings, violations);
        validateCopyLimit(printings, violations);
        validateCardLegality(printings, violations);
        return new DeckLegalityResponse(violations.isEmpty(), violations);
    }

    /**
     * Hook for format-specific rules applied to each printing in the main deck.
     *
     * @param printing the printing to check
     * @param violations the violation collector
     * @since 1.0
     */
    protected abstract void validatePrinting(
            CardPrinting printing, List<DeckLegalityResponse.Violation> violations);

    private List<CardPrinting> expandMainDeck(
            List<DeckCard> deckCards, List<DeckLegalityResponse.Violation> violations) {
        // ponytail: main deck only; sideboard section rules can be added when the domain models them.
        List<CardPrinting> printings = new ArrayList<>();
        for (DeckCard deckCard : deckCards) {
            if (deckCard.getDeckSection() != Section.MAIN_DECK) {
                continue;
            }
            CardPrinting printing =
                    cardPrintingRepository.findById(deckCard.getCardPrintingId()).orElse(null);
            if (printing == null) {
                add(violations, "CARD_NOT_FOUND", "Card printing not found: " + deckCard.getCardPrintingId());
                continue;
            }
            for (int i = 0; i < deckCard.getQuantity(); i++) {
                printings.add(printing);
            }
        }
        return printings;
    }

    private void validateDeckSize(
            List<CardPrinting> printings, List<DeckLegalityResponse.Violation> violations) {
        if (printings.size() < MINIMUM_DECK_SIZE) {
            add(
                    violations,
                    "DECK_SIZE_INVALID",
                    "Deck must contain at least "
                            + MINIMUM_DECK_SIZE
                            + " cards but contains "
                            + printings.size());
        }
    }

    private void validateCopyLimit(
            List<CardPrinting> printings, List<DeckLegalityResponse.Violation> violations) {
        Map<String, Integer> copiesByOracleId = new HashMap<>();
        Map<String, String> nameByOracleId = new HashMap<>();
        for (CardPrinting printing : printings) {
            Card card = printing.getCard();
            if (isBasicLand(card)) {
                continue;
            }
            String oracleId = card.getScryfallOracleId();
            copiesByOracleId.merge(oracleId, 1, Integer::sum);
            nameByOracleId.putIfAbsent(oracleId, card.getName());
        }
        copiesByOracleId.forEach(
                (oracleId, copies) -> {
                    if (copies > MAX_COPIES) {
                        add(
                                violations,
                                "COPY_LIMIT_VIOLATION",
                                "Card "
                                        + nameByOracleId.get(oracleId)
                                        + " appears "
                                        + copies
                                        + " times; at most "
                                        + MAX_COPIES
                                        + " copies are allowed");
                    }
                });
    }

    private void validateCardLegality(
            List<CardPrinting> printings, List<DeckLegalityResponse.Violation> violations) {
        String format = formatCode().toLowerCase(Locale.ROOT);
        Map<String, Boolean> checked = new HashMap<>();
        for (CardPrinting printing : printings) {
            Card card = printing.getCard();
            if (!checked.computeIfAbsent(
                    card.getScryfallOracleId(), id -> checkCardLegality(card, format, violations))) {
                continue;
            }
            validatePrinting(printing, violations);
        }
    }

    private boolean checkCardLegality(
            Card card, String format, List<DeckLegalityResponse.Violation> violations) {
        String prefix = formatCode();
        List<String> statuses =
                card.getLegalities().stream()
                        .filter(legality -> format.equalsIgnoreCase(legality.getFormatCode()))
                        .map(legality -> legality.getLegalityStatus().toLowerCase(Locale.ROOT))
                        .toList();
        if (statuses.isEmpty()) {
            add(
                    violations,
                    prefix + "_LEGALITY_UNKNOWN",
                    "Legality of " + card.getName() + " in " + format + " is unknown");
            return false;
        }
        if (!statuses.contains("legal")) {
            add(
                    violations,
                    prefix + "_LEGALITY_INVALID",
                    card.getName() + " is not legal in " + format);
            return false;
        }
        return true;
    }

    private boolean isBasicLand(Card card) {
        return card.getTypeLine() != null
                && card.getTypeLine().toLowerCase(Locale.ROOT).contains("basic land");
    }

    private void add(
            List<DeckLegalityResponse.Violation> violations, String code, String message) {
        violations.add(new DeckLegalityResponse.Violation(code, message));
    }
}
