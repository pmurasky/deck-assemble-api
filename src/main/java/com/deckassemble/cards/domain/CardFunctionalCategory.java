package com.deckassemble.cards.domain;

/**
 * Pure text-based functional classification shared by card search filtering and recommendation
 * scoring. Relocated from {@code recommendations.application.CardCategorizer} so both {@code cards}
 * and {@code recommendations} can depend on it without creating a cycle between those bounded
 * contexts (only {@code recommendations} may depend on {@code cards}).
 *
 * <p>Callers must pass already-lowercased type/oracle text; this class performs no
 * case-normalization itself, matching the original contract.
 */
public enum CardFunctionalCategory {
    LAND,
    RAMP,
    DRAW,
    WIPE,
    REMOVAL,
    SYNERGY;

    public static final String LAND_MARKER = "land";
    public static final String RAMP_MANA_MARKER = "add {";
    public static final String SEARCH_LIBRARY_MARKER = "search your library";
    public static final String DRAW_MARKER = "draw";
    public static final String DESTROY_ALL_MARKER = "destroy all";
    public static final String EXILE_ALL_MARKER = "exile all";
    public static final String DESTROY_TARGET_MARKER = "destroy target";
    public static final String EXILE_TARGET_MARKER = "exile target";

    public static CardFunctionalCategory categorize(String types, String text) {
        if (types.contains(LAND_MARKER)) {
            return LAND;
        }
        if (isRamp(text)) {
            return RAMP;
        }
        if (text.contains(DRAW_MARKER)) {
            return DRAW;
        }
        if (isWipe(text)) {
            return WIPE;
        }
        if (isRemoval(text)) {
            return REMOVAL;
        }
        return SYNERGY;
    }

    private static boolean isRamp(String text) {
        return text.contains(RAMP_MANA_MARKER)
                || (text.contains(SEARCH_LIBRARY_MARKER) && text.contains(LAND_MARKER));
    }

    private static boolean isWipe(String text) {
        return text.contains(DESTROY_ALL_MARKER) || text.contains(EXILE_ALL_MARKER);
    }

    private static boolean isRemoval(String text) {
        return text.contains(DESTROY_TARGET_MARKER) || text.contains(EXILE_TARGET_MARKER);
    }
}
