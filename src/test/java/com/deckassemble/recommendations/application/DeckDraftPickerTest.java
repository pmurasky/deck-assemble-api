package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeckDraftPickerTest {

    private long nextPrintingId = 1L;

    @Test
    void shouldFillQuotasThenSynergyWithRemainingSlots() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(40, Category.LAND));
        sorted.addAll(candidates(12, Category.RAMP));
        sorted.addAll(candidates(12, Category.DRAW));
        sorted.addAll(candidates(10, Category.REMOVAL));
        sorted.addAll(candidates(5, Category.WIPE));
        sorted.addAll(candidates(40, Category.SYNERGY));

        var picked = DeckDraftPicker.pick(sorted, 99);

        assertThat(picked).hasSize(99);
        assertThat(count(picked, Category.LAND)).isEqualTo(36);
        assertThat(count(picked, Category.RAMP)).isEqualTo(10);
        assertThat(count(picked, Category.DRAW)).isEqualTo(10);
        assertThat(count(picked, Category.REMOVAL)).isEqualTo(8);
        assertThat(count(picked, Category.WIPE)).isEqualTo(3);
        assertThat(count(picked, Category.SYNERGY)).isEqualTo(32);
    }

    @Test
    void shouldFlowShortfallToRemainingSlots() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(20, Category.LAND));
        sorted.addAll(candidates(40, Category.SYNERGY));

        var picked = DeckDraftPicker.pick(sorted, 60);

        assertThat(picked).hasSize(60);
        assertThat(count(picked, Category.LAND)).isEqualTo(20);
        assertThat(count(picked, Category.SYNERGY)).isEqualTo(40);
    }

    @Test
    void shouldStopAtSlotLimit() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(50, Category.LAND));

        var picked = DeckDraftPicker.pick(sorted, 10);

        assertThat(picked).hasSize(10);
    }

    @Test
    void shouldNotPickSameOracleTwice() {
        var first = candidate("Duplicate", Category.LAND);
        var second = candidate("Duplicate", Category.SYNERGY);

        var picked = DeckDraftPicker.pick(List.of(first, second), 2);

        assertThat(picked).hasSize(1);
    }

    private long count(List<DeckCandidate> picked, Category category) {
        return picked.stream().filter(candidate -> candidate.category() == category).count();
    }

    private List<DeckCandidate> candidates(int count, Category category) {
        var result = new ArrayList<DeckCandidate>();
        for (var index = 0; index < count; index++) {
            result.add(candidate(category.name() + index, category));
        }
        return result;
    }

    private DeckCandidate candidate(String name, Category category) {
        return new DeckCandidate(
                nextPrintingId++,
                new Card("oracle-" + name, name),
                category,
                new CardScore(0.5, 100L));
    }
}
