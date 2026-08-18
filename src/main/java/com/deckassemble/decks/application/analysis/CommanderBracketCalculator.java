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

    private CommanderBracketCalculator() {}

    static CommanderBracket bracket(List<AnalysisEntry> entries, int comboCount) {
        List<String> gameChangers = DeckCompositionCalculator.gameChangers(entries);
        return new CommanderBracket(bracketLevel(gameChangers.size(), comboCount), gameChangers);
    }

    private static int bracketLevel(int gameChangers, int comboCount) {
        if (gameChangers > BRACKET_4_MAX_GAME_CHANGERS) {
            return 5;
        }
        if (gameChangers > BRACKET_3_MAX_GAME_CHANGERS || comboCount >= COMBO_HEAVY_COUNT) {
            return 4;
        }
        if (gameChangers > 0) {
            return 3;
        }
        return comboCount > 0 ? 2 : 1;
    }
}
