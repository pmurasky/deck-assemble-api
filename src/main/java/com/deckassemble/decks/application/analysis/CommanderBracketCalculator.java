package com.deckassemble.decks.application.analysis;

import java.util.List;

/** Approximates the WotC Commander Bracket (1-5) from game changer and combo density. */
final class CommanderBracketCalculator {

    // ponytail: the WotC bracket rubric has no official numeric cutoffs; thresholds mirror the
    // published bracket descriptions (1-2 precon level, 3 = a few game changers, 4 = optimized,
    // 5 = cEDH). Revisit when WotC publishes machine-readable criteria.
    private static final int BRACKET_3_MAX_GAME_CHANGERS = 3;
    private static final int BRACKET_4_MAX_GAME_CHANGERS = 6;
    private static final int COMBO_HEAVY_COUNT = 3;
    private static final int BRACKET_UPGRADED = 3;
    private static final int BRACKET_OPTIMIZED = 4;
    private static final int BRACKET_CEDH = 5;

    private CommanderBracketCalculator() {}

    static CommanderBracket bracket(List<AnalysisEntry> entries, int comboCount) {
        List<String> gameChangers = DeckCompositionCalculator.gameChangers(entries);
        return new CommanderBracket(bracketLevel(gameChangers.size(), comboCount), gameChangers);
    }

    private static int bracketLevel(int gameChangers, int comboCount) {
        if (gameChangers > BRACKET_4_MAX_GAME_CHANGERS) {
            return BRACKET_CEDH;
        }
        if (gameChangers > BRACKET_3_MAX_GAME_CHANGERS || comboCount >= COMBO_HEAVY_COUNT) {
            return BRACKET_OPTIMIZED;
        }
        if (gameChangers > 0) {
            return BRACKET_UPGRADED;
        }
        return comboCount > 0 ? 2 : 1;
    }
}
