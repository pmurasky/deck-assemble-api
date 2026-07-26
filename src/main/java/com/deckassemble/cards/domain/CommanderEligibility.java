package com.deckassemble.cards.domain;

import org.jspecify.annotations.Nullable;

/**
 * Commander format eligibility predicate, shared by card search filters and deck building.
 * Eligible when any face is a legendary creature or any face says "can be your commander".
 */
public final class CommanderEligibility {

    private static final String CAN_BE_YOUR_COMMANDER = "can be your commander";

    private CommanderEligibility() {}

    public static boolean isEligible(Card card) {
        var text = new StringBuilder();
        appendText(text, card.getTypeLine());
        appendText(text, card.getOracleText());
        card.getFaces()
                .forEach(
                        face -> {
                            appendText(text, face.getTypeLine());
                            appendText(text, face.getOracleText());
                        });
        var content = text.toString();
        var legendaryCreature = content.contains("legendary") && content.contains("creature");
        return legendaryCreature || content.contains(CAN_BE_YOUR_COMMANDER);
    }

    private static void appendText(StringBuilder text, @Nullable String value) {
        if (value != null) {
            text.append(value.toLowerCase()).append(' ');
        }
    }
}
