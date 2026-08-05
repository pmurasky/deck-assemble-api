package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import java.util.Locale;
import org.springframework.stereotype.Component;

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
        if (types.contains("land")) {
            return Category.LAND;
        }
        if (isRamp(text)) {
            return Category.RAMP;
        }
        if (text.contains("draw")) {
            return Category.DRAW;
        }
        if (isWipe(text)) {
            return Category.WIPE;
        }
        if (isRemoval(text)) {
            return Category.REMOVAL;
        }
        return Category.SYNERGY;
    }

    private static boolean isRamp(String text) {
        return text.contains("add {")
                || (text.contains("search your library") && text.contains("land"));
    }

    private static boolean isWipe(String text) {
        return text.contains("destroy all") || text.contains("exile all");
    }

    private static boolean isRemoval(String text) {
        return text.contains("destroy target") || text.contains("exile target");
    }

    private static void appendLowercased(StringBuilder target, String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }
}
