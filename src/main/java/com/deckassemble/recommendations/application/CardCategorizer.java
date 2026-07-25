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
        var types = typeLine.toString();
        var text = oracleText.toString();
        if (types.contains("land")) {
            return Category.LAND;
        }
        if (text.contains("add {") || (text.contains("search your library") && text.contains("land"))) {
            return Category.RAMP;
        }
        if (text.contains("draw")) {
            return Category.DRAW;
        }
        if (text.contains("destroy all") || text.contains("exile all")) {
            return Category.WIPE;
        }
        if (text.contains("destroy target") || text.contains("exile target")) {
            return Category.REMOVAL;
        }
        return Category.SYNERGY;
    }

    private static void appendLowercased(StringBuilder target, String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }
}
