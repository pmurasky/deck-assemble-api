package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        assertThat(picked).hasSize(44);
        assertThat(count(picked, Category.LAND)).isEqualTo(20);
        assertThat(count(picked, Category.SYNERGY)).isEqualTo(24);
    }

    @Test
    void shouldReserveSlotsForBasicLandsWhenCandidateLandsFallShort() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(6, Category.LAND, 0.0));
        sorted.addAll(candidates(93, Category.SYNERGY, 1.0));

        var picked = DeckDraftPicker.pick(sorted, 99);

        assertThat(picked).hasSize(69);
        assertThat(count(picked, Category.LAND)).isEqualTo(6);
        assertThat(count(picked, Category.SYNERGY)).isEqualTo(63);
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

    @Test
    void shouldPreferUnfilledRoleOverExtraFilledRoleCard() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(9, Category.REMOVAL, 0.9));
        sorted.add(candidate("Draw Spell", Category.DRAW, 0.4));

        var picked = DeckDraftPicker.pick(sorted, 9, quotasWithoutLands());

        assertThat(count(picked, Category.REMOVAL)).isEqualTo(8);
        assertThat(count(picked, Category.DRAW)).isEqualTo(1);
    }

    @Test
    void shouldFillMultiRoleCardIntoHighestNeedRole() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(10, Category.RAMP));
        var multi =
                new DeckCandidate(
                        nextPrintingId++,
                        new Card("oracle-multi", "Multi"),
                        Category.RAMP,
                        new CardScore(0.5, 100L),
                        List.of(),
                        Set.of(Category.RAMP, Category.DRAW));
        sorted.add(multi);
        sorted.addAll(candidates(9, Category.DRAW));

        var picked = DeckDraftPicker.pick(sorted, 20, quotasWithoutLands());

        assertThat(picked).contains(multi);
        assertThat(count(picked, Category.RAMP)).isEqualTo(11);
        assertThat(count(picked, Category.DRAW)).isEqualTo(9);
    }

    @Test
    void shouldPenalizeCurveCongestion() {
        var sorted = new ArrayList<DeckCandidate>();
        for (var index = 0; index < 16; index++) {
            sorted.add(candidateWithMv("Two Drop " + index, 2));
        }
        var fourDrop = candidateWithMv("Four Drop", 4);
        sorted.add(fourDrop);

        var picked = DeckDraftPicker.pick(sorted, 16, quotasWithoutLands());

        assertThat(picked).contains(fourDrop);
        assertThat(count(picked, Category.SYNERGY)).isEqualTo(16);
    }

    @Test
    void shouldHonorAdjustedQuotas() {
        var sorted = new ArrayList<DeckCandidate>();
        sorted.addAll(candidates(10, Category.FINISHER));
        sorted.addAll(candidates(20, Category.SYNERGY));
        var quotas = new EnumMap<Category, Integer>(DeckDraftPicker.QUOTAS);
        quotas.put(Category.LAND, 0);
        quotas.put(Category.FINISHER, 6);

        var picked = DeckDraftPicker.pick(sorted, 20, quotas);

        assertThat(count(picked, Category.FINISHER)).isEqualTo(6);
        assertThat(count(picked, Category.SYNERGY)).isEqualTo(14);
    }

    @Test
    void shouldPreserveScoreExplanationsWhenPicking() {
        var contribution =
                new ScoreContribution(
                        RecommendationReasonCode.COMMANDER_SYNERGY,
                        new BigDecimal("0.50"),
                        Map.of("synergy", "0.5"));
        var candidate =
                new DeckCandidate(
                        nextPrintingId++,
                        new Card("oracle-x", "X"),
                        Category.SYNERGY,
                        new CardScore(0.5, 100L),
                        List.of(contribution));

        var picked = DeckDraftPicker.pick(List.of(candidate), 1, quotasWithoutLands());

        assertThat(picked).hasSize(1);
        assertThat(picked.get(0).contributions()).containsExactly(contribution);
        assertThat(picked.get(0).totalScore()).isEqualByComparingTo("0.50");
        assertThat(picked.get(0).roles()).containsExactly(Category.SYNERGY);
    }

    private long count(List<DeckCandidate> picked, Category category) {
        return picked.stream().filter(candidate -> candidate.category() == category).count();
    }

    private Map<Category, Integer> quotasWithoutLands() {
        var quotas = new EnumMap<Category, Integer>(DeckDraftPicker.QUOTAS);
        quotas.put(Category.LAND, 0);
        return quotas;
    }

    private List<DeckCandidate> candidates(int count, Category category) {
        return candidates(count, category, 0.5);
    }

    private List<DeckCandidate> candidates(int count, Category category, double synergy) {
        var result = new ArrayList<DeckCandidate>();
        for (var index = 0; index < count; index++) {
            result.add(candidate(category.name() + index, category, synergy));
        }
        return result;
    }

    private DeckCandidate candidate(String name, Category category) {
        return candidate(name, category, 0.5);
    }

    private DeckCandidate candidate(String name, Category category, double synergy) {
        return new DeckCandidate(
                nextPrintingId++,
                new Card("oracle-" + name, name),
                category,
                new CardScore(synergy, 100L));
    }

    private DeckCandidate candidateWithMv(String name, int manaValue) {
        var card = new Card("oracle-" + name, name);
        card.setManaValue(BigDecimal.valueOf(manaValue));
        return new DeckCandidate(
                nextPrintingId++, card, Category.SYNERGY, new CardScore(0.5, 100L));
    }
}
