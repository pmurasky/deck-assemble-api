package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurnStepTest {

    @Test
    void shouldCycleThroughAllStepsInOrder() {
        TurnStep step = new TurnStep.Untap();
        StringBuilder order = new StringBuilder(step.stepName());
        for (int i = 0; i < 11; i++) {
            step = step.next();
            order.append(" -> ").append(step.stepName());
        }
        assertThat(order.toString())
                .isEqualTo(
                        "Untap -> Upkeep -> Draw -> FirstMain -> BeginCombat -> DeclareAttackers"
                                + " -> DeclareBlockers -> CombatDamage -> EndCombat -> SecondMain"
                                + " -> End -> Cleanup");
    }

    @Test
    void shouldWrapToUntapAfterCleanup() {
        assertThat(new TurnStep.Cleanup().next()).isInstanceOf(TurnStep.Untap.class);
    }

    @Test
    void shouldExposeSimpleRecordNameAsStepName() {
        assertThat(new TurnStep.FirstMain().stepName()).isEqualTo("FirstMain");
    }
}
