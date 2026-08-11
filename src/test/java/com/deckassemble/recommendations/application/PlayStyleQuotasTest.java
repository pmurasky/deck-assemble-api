package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayStyleQuotasTest {

    @Test
    void shouldReturnBaseQuotasWhenPlayStyleMissing() {
        assertThat(PlayStyleQuotas.forStyle(null)).isEqualTo(DeckDraftPicker.QUOTAS);
        assertThat(PlayStyleQuotas.forStyle(" ")).isEqualTo(DeckDraftPicker.QUOTAS);
    }

    @Test
    void shouldReturnBaseQuotasForMidrangeAndUnknownStyles() {
        assertThat(PlayStyleQuotas.forStyle("midrange")).isEqualTo(DeckDraftPicker.QUOTAS);
        assertThat(PlayStyleQuotas.forStyle("storm")).isEqualTo(DeckDraftPicker.QUOTAS);
    }

    @Test
    void shouldMatchStyleCaseInsensitively() {
        assertThat(PlayStyleQuotas.forStyle("AGGRO")).isEqualTo(PlayStyleQuotas.forStyle("aggro"));
    }

    @Test
    void shouldBoostFinishersForAggro() {
        var quotas = PlayStyleQuotas.forStyle("aggro");

        assertThat(quotas.get(Category.FINISHER)).isEqualTo(7);
        assertThat(quotas.get(Category.RAMP)).isEqualTo(11);
        assertThat(quotas.get(Category.DRAW)).isEqualTo(8);
        assertThat(quotas.get(Category.WIPE)).isEqualTo(2);
        assertThat(quotas.get(Category.PROTECTION)).isEqualTo(4);
        assertThat(quotas.get(Category.LAND)).isEqualTo(36);
    }

    @Test
    void shouldBoostInteractionForControl() {
        var quotas = PlayStyleQuotas.forStyle("control");

        assertThat(quotas.get(Category.WIPE)).isEqualTo(5);
        assertThat(quotas.get(Category.REMOVAL)).isEqualTo(10);
        assertThat(quotas.get(Category.DRAW)).isEqualTo(11);
        assertThat(quotas.get(Category.FINISHER)).isEqualTo(1);
        assertThat(quotas.get(Category.PROTECTION)).isEqualTo(3);
    }

    @Test
    void shouldBoostDrawAndProtectionForCombo() {
        var quotas = PlayStyleQuotas.forStyle("combo");

        assertThat(quotas.get(Category.DRAW)).isEqualTo(12);
        assertThat(quotas.get(Category.PROTECTION)).isEqualTo(7);
        assertThat(quotas.get(Category.FINISHER)).isEqualTo(2);
        assertThat(quotas.get(Category.REMOVAL)).isEqualTo(6);
    }

    @Test
    void shouldKeepQuotaTotalConstantAcrossStyles() {
        var base = total(DeckDraftPicker.QUOTAS);

        for (var style : new String[] {"aggro", "control", "combo", "midrange", "tribal"}) {
            assertThat(total(PlayStyleQuotas.forStyle(style))).isEqualTo(base);
        }
    }

    private static int total(Map<Category, Integer> quotas) {
        return quotas.values().stream().mapToInt(Integer::intValue).sum();
    }
}
