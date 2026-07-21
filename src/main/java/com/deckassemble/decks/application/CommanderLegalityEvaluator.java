package com.deckassemble.decks.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.decks.api.DeckLegalityResponse;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
class CommanderLegalityEvaluator {

    private static final int COMMANDER_DECK_SIZE = 100;

    private final CardRepository cardRepository;
    private final CardPrintingRepository cardPrintingRepository;

    CommanderLegalityEvaluator(
            CardRepository cardRepository, CardPrintingRepository cardPrintingRepository) {
        this.cardRepository = cardRepository;
        this.cardPrintingRepository = cardPrintingRepository;
    }

    DeckLegalityResponse evaluate(Deck deck, List<DeckCard> deckCards) {
        var violations = new ArrayList<DeckLegalityResponse.Violation>();
        if (!"COMMANDER".equalsIgnoreCase(deck.getFormatCode())) {
            add(violations, "COMMANDER_FORMAT_REQUIRED", "Deck format must be COMMANDER.");
        }
        Card commander = commander(deck.getCommanderCardId(), "COMMANDER", violations);
        Card partner =
                commander(deck.getSecondaryCommanderCardId(), "SECONDARY_COMMANDER", violations);
        validateCommanders(commander, partner, violations);
        List<Card> mainDeck = mainDeck(deckCards, violations);
        validateCards(mainDeck, commander, partner, violations);
        validateDeckSize(
                mainDeck,
                deck.getCommanderCardId(),
                deck.getSecondaryCommanderCardId(),
                violations);
        return new DeckLegalityResponse(violations.isEmpty(), violations);
    }

    private Card commander(
            Long cardId, String role, List<DeckLegalityResponse.Violation> violations) {
        if (cardId == null) {
            if ("COMMANDER".equals(role)) {
                add(violations, "COMMANDER_REQUIRED", "A commander must be selected.");
            }
            return null;
        }
        return cardRepository
                .findById(cardId)
                .orElseGet(
                        () -> {
                            add(
                                    violations,
                                    role + "_NOT_FOUND",
                                    role + " card could not be found.");
                            return null;
                        });
    }

    private void validateCommanders(
            Card commander, Card partner, List<DeckLegalityResponse.Violation> violations) {
        if (commander == null) {
            return;
        }
        boolean paired = partner != null && validPair(commander, partner);
        validateCommander(commander, "COMMANDER", violations);
        if (partner != null && !eligible(partner) && !paired) {
            add(
                    violations,
                    "SECONDARY_COMMANDER_INELIGIBLE",
                    partner.getName() + " cannot be a commander.");
        }
        if (partner != null && !paired) {
            add(violations, "COMMANDER_PAIR_INVALID", "The selected commanders cannot be paired.");
        }
        if (partner != null) {
            validateCommander(partner, "SECONDARY_COMMANDER", violations);
        }
    }

    private void validateCommander(
            Card card, String role, List<DeckLegalityResponse.Violation> violations) {
        if (!eligible(card)) {
            add(violations, role + "_INELIGIBLE", card.getName() + " cannot be a commander.");
        }
        validateLegality(card, violations);
    }

    private List<Card> mainDeck(
            List<DeckCard> deckCards, List<DeckLegalityResponse.Violation> violations) {
        return deckCards.stream()
                .filter(card -> card.getDeckSection() == DeckCard.Section.MAIN_DECK)
                .flatMap(card -> cardsFor(card, violations).stream())
                .toList();
    }

    private List<Card> cardsFor(
            DeckCard deckCard, List<DeckLegalityResponse.Violation> violations) {
        return cardPrintingRepository
                .findById(deckCard.getCardPrintingId())
                .map(CardPrinting::getCard)
                .map(card -> java.util.Collections.nCopies(deckCard.getQuantity(), card))
                .orElseGet(
                        () -> {
                            add(
                                    violations,
                                    "CARD_NOT_FOUND",
                                    "A deck card printing could not be found.");
                            return List.of();
                        });
    }

    private void validateCards(
            List<Card> cards,
            Card commander,
            Card partner,
            List<DeckLegalityResponse.Violation> violations) {
        Set<String> colorIdentity = colors(commander, partner);
        cards.forEach(card -> validateCard(card, colorIdentity, violations));
        cards.stream()
                .filter(card -> !basicLand(card))
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                Card::getScryfallOracleId, java.util.stream.Collectors.counting()))
                .forEach(
                        (oracleId, count) -> {
                            if (count > 1) {
                                add(
                                        violations,
                                        "SINGLETON_VIOLATION",
                                        "A non-basic card appears more than once.");
                            }
                        });
    }

    private void validateCard(
            Card card, Set<String> colorIdentity, List<DeckLegalityResponse.Violation> violations) {
        validateLegality(card, violations);
        if (!colorIdentity.containsAll(colors(card))) {
            add(
                    violations,
                    "COLOR_IDENTITY_VIOLATION",
                    card.getName() + " is outside the commander's color identity.");
        }
    }

    private void validateLegality(Card card, List<DeckLegalityResponse.Violation> violations) {
        String status =
                card.getLegalities().stream()
                        .filter(legality -> "commander".equalsIgnoreCase(legality.getFormatCode()))
                        .map(legality -> legality.getLegalityStatus())
                        .findFirst()
                        .orElse(null);
        if (status == null) {
            add(
                    violations,
                    "COMMANDER_LEGALITY_UNKNOWN",
                    card.getName() + " has no Commander legality data.");
        } else if (!"legal".equalsIgnoreCase(status)) {
            add(
                    violations,
                    "COMMANDER_LEGALITY_INVALID",
                    card.getName() + " is not legal in Commander.");
        }
    }

    private void validateDeckSize(
            List<Card> cards,
            Long commander,
            Long partner,
            List<DeckLegalityResponse.Violation> violations) {
        int commanderCount = (commander == null ? 0 : 1) + (partner == null ? 0 : 1);
        if (cards.size() + commanderCount != COMMANDER_DECK_SIZE) {
            add(
                    violations,
                    "DECK_SIZE_INVALID",
                    "A Commander deck must contain exactly 100 cards.");
        }
    }

    private boolean eligible(Card card) {
        return text(card.getTypeLine()).contains("legendary")
                        && text(card.getTypeLine()).contains("creature")
                || text(card.getOracleText()).contains("can be your commander");
    }

    private boolean validPair(Card first, Card second) {
        return genericPartnerPair(first, second)
                || keywordPair(first, second, "friends forever")
                || namedPartnerPair(first, second)
                || rolePair(first, second, this::chooseBackground, this::background)
                || rolePair(first, second, this::doctorsCompanion, this::timeLordDoctor);
    }

    private boolean genericPartnerPair(Card first, Card second) {
        return genericPartner(first) && genericPartner(second);
    }

    private boolean keywordPair(Card first, Card second, String keyword) {
        return has(first, keyword) && has(second, keyword);
    }

    private boolean namedPartnerPair(Card first, Card second) {
        return partnerWith(first, second) || partnerWith(second, first);
    }

    private boolean rolePair(
            Card first, Card second, Predicate<Card> firstRole, Predicate<Card> secondRole) {
        return firstRole.test(first) && secondRole.test(second)
                || firstRole.test(second) && secondRole.test(first);
    }

    private boolean partnerWith(Card first, Card second) {
        return text(first.getOracleText()).contains("partner with " + text(second.getName()));
    }

    private boolean genericPartner(Card card) {
        return text(card.getOracleText())
                .lines()
                .map(String::trim)
                .anyMatch(line -> line.equals("partner") || line.startsWith("partner ("));
    }

    private boolean chooseBackground(Card card) {
        return has(card, "choose a background");
    }

    private boolean background(Card card) {
        return text(card.getTypeLine()).contains("legendary enchantment")
                && has(card, "background");
    }

    private boolean doctorsCompanion(Card card) {
        return has(card, "doctor's companion");
    }

    private boolean timeLordDoctor(Card card) {
        return text(card.getTypeLine()).contains("legendary")
                && has(card, "time lord")
                && has(card, "doctor");
    }

    private boolean basicLand(Card card) {
        return text(card.getTypeLine()).contains("basic land");
    }

    private Set<String> colors(Card... cards) {
        return java.util.Arrays.stream(cards)
                .filter(card -> card != null)
                .flatMap(card -> colors(card).stream())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<String> colors(Card card) {
        if (card == null || card.getColorIdentity() == null || card.getColorIdentity().isBlank()) {
            return Set.of();
        }
        return Set.of(card.getColorIdentity().toUpperCase(Locale.ROOT).split(","));
    }

    private boolean has(Card card, String text) {
        return text(card.getOracleText()).contains(text);
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void add(List<DeckLegalityResponse.Violation> violations, String code, String message) {
        violations.add(new DeckLegalityResponse.Violation(code, message));
    }
}
