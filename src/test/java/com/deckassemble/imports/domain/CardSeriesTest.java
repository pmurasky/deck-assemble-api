package com.deckassemble.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardSeriesTest {

    @Test
    void shouldResolveSeriesByKeyCaseInsensitively() {
        assertThat(CardSeries.fromKey("tmnt")).contains(CardSeries.TMNT);
        assertThat(CardSeries.fromKey("Marvel")).contains(CardSeries.MARVEL);
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {
        assertThat(CardSeries.fromKey("pokemon")).isEmpty();
    }

    @Test
    void shouldExposeDisplayLabels() {
        assertThat(CardSeries.ASSASSINS_CREED.label()).isEqualTo("Assassin's Creed");
        assertThat(CardSeries.SPIDER_MAN.label()).isEqualTo("Spider-Man");
    }

    @Test
    void shouldJoinMultipleSeriesIntoSingleQueryFragment() {
        String fragment = CardSeries.toQueryFragment(List.of(CardSeries.HOBBIT, CardSeries.TMNT));

        assertThat(fragment).isEqualTo("e:hob,hoc,tmt,tmc");
    }

    @Test
    void shouldDeduplicateSetCodesSharedAcrossSeries() {
        String fragment =
                CardSeries.toQueryFragment(List.of(CardSeries.MARVEL, CardSeries.SPIDER_MAN));

        assertThat(fragment).isEqualTo("e:mar,msh,msc,spm,spe");
    }

    @Test
    void shouldRejectEmptySeriesSelection() {
        assertThatThrownBy(() -> CardSeries.toQueryFragment(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
