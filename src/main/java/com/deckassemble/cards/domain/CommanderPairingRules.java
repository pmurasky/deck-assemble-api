package com.deckassemble.cards.domain;

import java.util.Locale;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CommanderPairingRules {

    public boolean canPair(Card first, Card second) {
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

    private boolean has(Card card, String text) {
        return text(card.getOracleText()).contains(text);
    }

    private String text(@Nullable String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
