package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardFunctionalCategory;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// Text classification logic lives in cards.domain.CardFunctionalCategory (shared with card
// search filtering); this class is a thin, API-compatible wrapper kept for existing callers.
@Component
public class CardCategorizer {

    public enum Category {
        LAND,
        RAMP,
        DRAW,
        WIPE,
        REMOVAL,
        PROTECTION,
        FINISHER,
        SYNERGY
    }

    public Category categorize(Card card) {
        return toCategory(CardFunctionalCategory.prioritize(categorizeDomain(card)));
    }

    public Set<Category> categorizeAll(Card card) {
        return categorizeDomain(card).stream()
                .map(CardCategorizer::toCategory)
                .collect(Collectors.toSet());
    }

    public static Category categorizeText(String types, String text) {
        return toCategory(CardFunctionalCategory.categorize(types, text));
    }

    private static Set<CardFunctionalCategory> categorizeDomain(Card card) {
        var typeLine = new StringBuilder();
        var oracleText = new StringBuilder();
        for (CardFace face : card.getFaces()) {
            appendLowercased(typeLine, face.getTypeLine());
            appendLowercased(oracleText, face.getOracleText());
        }
        return CardFunctionalCategory.categorizeAll(
                typeLine.toString(), oracleText.toString(), card.getOracleTags());
    }

    private static Category toCategory(CardFunctionalCategory category) {
        return switch (category) {
            case LAND -> Category.LAND;
            case RAMP -> Category.RAMP;
            case DRAW -> Category.DRAW;
            case WIPE -> Category.WIPE;
            case REMOVAL -> Category.REMOVAL;
            case PROTECTION -> Category.PROTECTION;
            case FINISHER -> Category.FINISHER;
            case SYNERGY -> Category.SYNERGY;
        };
    }

    private static void appendLowercased(StringBuilder target, String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }
}
