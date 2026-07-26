package com.deckassemble.cards.domain;

/**
 * Commander format eligibility predicate, shared by card search filters and deck building.
 * Eligible when any face is a legendary creature or any face says "can be your commander".
 */
public final class CommanderEligibility {

    private static final String CAN_BE_YOUR_COMMANDER = "can be your commander";

    private CommanderEligibility() {}

    public static boolean isEligible(Card card) {
        var text = new StringBuilder();
        card.getFaces().forEach(face -> appendFaceText(text, face));
        var content = text.toString();
        var legendaryCreature = content.contains("legendary") && content.contains("creature");
        return legendaryCreature || content.contains(CAN_BE_YOUR_COMMANDER);
    }

    private static void appendFaceText(StringBuilder text, CardFace face) {
        if (face.getTypeLine() != null) {
            text.append(face.getTypeLine().toLowerCase()).append(' ');
        }
        if (face.getOracleText() != null) {
            text.append(face.getOracleText().toLowerCase()).append(' ');
        }
    }
}
