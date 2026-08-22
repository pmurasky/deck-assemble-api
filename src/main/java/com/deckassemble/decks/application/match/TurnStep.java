package com.deckassemble.decks.application.match;

/**
 * The twelve steps of a two-player Commander match turn, in order. Sealed so {@link #next()} can
 * be an exhaustive pattern-matching switch with no default branch. {@code Cleanup.next()} wraps
 * back to {@link Untap}; the {@link Match} aggregate owns the turn boundary (incrementing the
 * turn number and switching the active player) when it sees that wrap.
 */
public sealed interface TurnStep {

    record Untap() implements TurnStep {}

    record Upkeep() implements TurnStep {}

    record Draw() implements TurnStep {}

    record FirstMain() implements TurnStep {}

    record BeginCombat() implements TurnStep {}

    record DeclareAttackers() implements TurnStep {}

    record DeclareBlockers() implements TurnStep {}

    record CombatDamage() implements TurnStep {}

    record EndCombat() implements TurnStep {}

    record SecondMain() implements TurnStep {}

    record End() implements TurnStep {}

    record Cleanup() implements TurnStep {}

    default TurnStep next() {
        return switch (this) {
            case Untap ignored -> new Upkeep();
            case Upkeep ignored -> new Draw();
            case Draw ignored -> new FirstMain();
            case FirstMain ignored -> new BeginCombat();
            case BeginCombat ignored -> new DeclareAttackers();
            case DeclareAttackers ignored -> new DeclareBlockers();
            case DeclareBlockers ignored -> new CombatDamage();
            case CombatDamage ignored -> new EndCombat();
            case EndCombat ignored -> new SecondMain();
            case SecondMain ignored -> new End();
            case End ignored -> new Cleanup();
            case Cleanup ignored -> new Untap();
        };
    }

    default String stepName() {
        return getClass().getSimpleName();
    }
}
