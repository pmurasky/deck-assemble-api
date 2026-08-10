package com.deckassemble.cards.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Functional classification shared by card search filtering and recommendation scoring. Tags
 * (Tagger oracle labels) and oracle-text markers both contribute; a card may fill several roles.
 * The legacy single-label {@link #categorize(String, String)} contract is preserved for existing
 * callers.
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
    PROTECTION,
    FINISHER,
    SYNERGY;

    public static final String LAND_MARKER = "land";
    public static final String RAMP_MANA_MARKER = "add {";
    public static final String SEARCH_LIBRARY_MARKER = "search your library";
    public static final String DRAW_MARKER = "draw";
    public static final String DESTROY_ALL_MARKER = "destroy all";
    public static final String EXILE_ALL_MARKER = "exile all";
    public static final String DESTROY_TARGET_MARKER = "destroy target";
    public static final String EXILE_TARGET_MARKER = "exile target";
    public static final String WIN_THE_GAME_MARKER = "you win the game";

    private static final Set<String> RAMP_TAGS = Set.of("ramp", "land ramp", "multi land ramp");
    private static final Set<String> DRAW_TAGS =
            Set.of("draw", "burst draw", "draw engine", "impulsive draw");
    private static final Set<String> REMOVAL_TAGS = Set.of("removal", "multi removal");
    private static final String REMOVAL_TAG_PREFIX = "removal-";
    private static final String COUNTERSPELL_TAG_PREFIX = "counterspell";
    private static final Set<String> WIPE_TAGS = Set.of("board-reset");
    private static final String SWEEPER_TAG_PREFIX = "sweeper";
    private static final Set<String> PROTECTION_TAGS = Set.of("protection", "gives protection");
    private static final String PROTECTS_TAG_PREFIX = "protects-";
    private static final String FINISHER_TAG = "alternate win condition";

    // ponytail: single-label priority for legacy callers; P2's picker consumes categorizeAll
    private static final List<CardFunctionalCategory> SINGLE_LABEL_PRIORITY =
            List.of(LAND, RAMP, DRAW, WIPE, REMOVAL, PROTECTION, FINISHER, SYNERGY);

    private static final List<TagRule> TAG_RULES =
            List.of(
                    new TagRule(RAMP_TAGS::contains, RAMP),
                    new TagRule(DRAW_TAGS::contains, DRAW),
                    new TagRule(CardFunctionalCategory::isWipeTag, WIPE),
                    new TagRule(CardFunctionalCategory::isRemovalTag, REMOVAL),
                    new TagRule(CardFunctionalCategory::isProtectionTag, PROTECTION),
                    new TagRule(FINISHER_TAG::equals, FINISHER));

    private record TagRule(
            java.util.function.Predicate<String> matcher, CardFunctionalCategory category) {}

    public static CardFunctionalCategory categorize(String types, String text) {
        Set<CardFunctionalCategory> all = categorizeAll(types, text, null);
        return SINGLE_LABEL_PRIORITY.stream().filter(all::contains).findFirst().orElse(SYNERGY);
    }

    public static Set<CardFunctionalCategory> categorizeAll(
            String types, String text, @Nullable String tagsCsv) {
        Set<CardFunctionalCategory> categories = EnumSet.noneOf(CardFunctionalCategory.class);
        if (types.contains(LAND_MARKER)) {
            categories.add(LAND);
        }
        addFromText(text, categories);
        if (tagsCsv != null) {
            for (String tag : tagsCsv.split(",")) {
                addFromTag(tag.trim(), categories);
            }
        }
        if (categories.isEmpty()) {
            categories.add(SYNERGY);
        }
        return categories;
    }

    private static void addFromText(String text, Set<CardFunctionalCategory> categories) {
        if (isRamp(text)) {
            categories.add(RAMP);
        }
        if (text.contains(DRAW_MARKER)) {
            categories.add(DRAW);
        }
        if (isWipe(text)) {
            categories.add(WIPE);
        }
        if (isRemoval(text)) {
            categories.add(REMOVAL);
        }
        if (text.contains(WIN_THE_GAME_MARKER)) {
            categories.add(FINISHER);
        }
    }

    private static void addFromTag(String tag, Set<CardFunctionalCategory> categories) {
        if (tag.isEmpty()) {
            return;
        }
        TAG_RULES.stream()
                .filter(rule -> rule.matcher().test(tag))
                .findFirst()
                .ifPresent(rule -> categories.add(rule.category()));
    }

    private static boolean isWipeTag(String tag) {
        return tag.startsWith(SWEEPER_TAG_PREFIX) || WIPE_TAGS.contains(tag);
    }

    // ponytail: counterspells count as interaction (REMOVAL) until P2 splits an INTERACTION
    // category
    private static boolean isRemovalTag(String tag) {
        return REMOVAL_TAGS.contains(tag)
                || tag.startsWith(REMOVAL_TAG_PREFIX)
                || tag.startsWith(COUNTERSPELL_TAG_PREFIX);
    }

    private static boolean isProtectionTag(String tag) {
        return PROTECTION_TAGS.contains(tag) || tag.startsWith(PROTECTS_TAG_PREFIX);
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
