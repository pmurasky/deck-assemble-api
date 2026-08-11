package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.domain.ManaPips;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BasicLandAllocationTest {

    @Test
    void shouldAllocateAllBasicsToSingleColorIdentity() {
        // Given
        var identity = Set.of("U");
        var pips = new ManaPips(0, 12, 0, 0, 0);

        // When
        var allocation = BasicLandAllocation.byPips(identity, pips, 9);

        // Then
        assertThat(allocation).isEqualTo(Map.of("U", 9));
    }

    @Test
    void shouldAllocateProportionallyToPipDemand() {
        // Given
        var identity = Set.of("U", "B");
        var pips = new ManaPips(0, 20, 10, 0, 0);

        // When
        var allocation = BasicLandAllocation.byPips(identity, pips, 9);

        // Then
        assertThat(allocation).isEqualTo(expected("U", 6, "B", 3));
    }

    @Test
    void shouldBreakRemainderTiesInWubrgOrder() {
        // Given
        var identity = Set.of("W", "U", "B");
        var pips = new ManaPips(1, 1, 1, 0, 0);

        // When
        var allocation = BasicLandAllocation.byPips(identity, pips, 4);

        // Then
        assertThat(allocation).isEqualTo(expected("W", 2, "U", 1, "B", 1));
    }

    @Test
    void shouldGiveNoBasicsToColorWithoutPips() {
        // Given
        var identity = Set.of("W", "U");
        var pips = new ManaPips(0, 7, 0, 0, 0);

        // When
        var allocation = BasicLandAllocation.byPips(identity, pips, 5);

        // Then
        assertThat(allocation).isEqualTo(Map.of("U", 5));
    }

    @Test
    void shouldSplitEvenlyWhenDeckHasNoColoredPips() {
        // Given
        var identity = Set.of("W", "U", "B");
        var pips = new ManaPips(0, 0, 0, 0, 0);

        // When
        var allocation = BasicLandAllocation.byPips(identity, pips, 5);

        // Then
        assertThat(allocation).isEqualTo(expected("W", 2, "U", 2, "B", 1));
    }

    @Test
    void shouldReturnEmptyMapWhenNoBasicsNeeded() {
        // Given
        var identity = Set.of("G");
        var pips = new ManaPips(0, 0, 0, 0, 4);

        // When
        var allocation = BasicLandAllocation.byPips(identity, pips, 0);

        // Then
        assertThat(allocation).isEmpty();
    }

    private static Map<String, Integer> expected(
            String color1, int count1, String color2, int count2) {
        var expected = new LinkedHashMap<String, Integer>();
        expected.put(color1, count1);
        expected.put(color2, count2);
        return expected;
    }

    private static Map<String, Integer> expected(
            String color1, int count1, String color2, int count2, String color3, int count3) {
        var expected = expected(color1, count1, color2, count2);
        expected.put(color3, count3);
        return expected;
    }
}
