package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardFunctionalCategory;
import java.util.Locale;
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
        SYNERGY
    }

    public Category categorize(Card card) {
        var typeLine = new StringBuilder();
        var oracleText = new StringBuilder();
        for (CardFace face : card.getFaces()) {
            appendLowercased(typeLine, face.getTypeLine());
            appendLowercased(oracleText, face.getOracleText());
        }
        return categorizeText(typeLine.toString(), oracleText.toString());
    }

    public static Category categorizeText(String types, String text) {
        return toCategory(CardFunctionalCategory.categorize(types, text));
    }

    private static Category toCategory(CardFunctionalCategory category) {
        return switch (category) {
            case LAND -> Category.LAND;
            case RAMP -> Category.RAMP;
            case DRAW -> Category.DRAW;
            case WIPE -> Category.WIPE;
            case REMOVAL -> Category.REMOVAL;
            case SYNERGY -> Category.SYNERGY;
        };
    }

    private static void appendLowercased(StringBuilder target, String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }
}
