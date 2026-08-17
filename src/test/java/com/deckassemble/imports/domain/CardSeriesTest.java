package com.deckassemble.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            textBlock =
                    """
                    LORWYN_ECLIPSED|Lorwyn Eclipsed|ecl,ecc,spg
                    AETHERDRIFT|Aetherdrift|dft,drc
                    ZENDIKAR_RISING|Zendikar Rising|znr
                    TALES_OF_MIDDLE_EARTH_COMMANDER|Tales of Middle-earth Commander|ltc
                    FOUNDATIONS|Foundations|fdn
                    BLOOMBURROW|Bloomburrow|blb
                    """)
    void shouldExposeClarifiedSeriesMappings(String key, String label, String codes) {
        CardSeries series = CardSeries.valueOf(key);

        assertThat(CardSeries.fromKey(key)).contains(series);
        assertThat(series.label()).isEqualTo(label);
        assertThat(series.setCodes()).containsExactly(codes.split(","));
        assertThat(CardSeries.toQueryFragment(List.of(series))).isEqualTo("e:" + codes);
        assertThat(newSeriesSetCodes()).doesNotHaveDuplicates();
    }

    private List<String> newSeriesSetCodes() {
        return List.of(
                        "LORWYN_ECLIPSED",
                        "AETHERDRIFT",
                        "ZENDIKAR_RISING",
                        "TALES_OF_MIDDLE_EARTH_COMMANDER",
                        "FOUNDATIONS",
                        "BLOOMBURROW")
                .stream()
                .map(CardSeries::valueOf)
                .flatMap(series -> series.setCodes().stream())
                .toList();
    }

    @Test
    void shouldRejectEmptySeriesSelection() {
        assertThatThrownBy(() -> CardSeries.toQueryFragment(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
