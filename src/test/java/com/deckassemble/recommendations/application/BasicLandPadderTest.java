package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BasicLandPadderTest {

    private static final long ISLAND_PRINTING = 11L;
    private static final long SWAMP_PRINTING = 22L;
    private static final long PLAINS_PRINTING = 33L;

    @Mock private CardCatalogService cardCatalogService;
    @InjectMocks private BasicLandPadder padder;

    @Test
    void shouldWeightBasicsByPipDemand() {
        // Given
        var basics = basicsInCatalog(true, true, false);
        var picked = new ArrayList<DeckCandidate>();
        picked.addAll(picked("{2}{U}", 40));
        picked.addAll(picked("{2}{B}", 20));

        // When
        var result = padder.pad(picked, Set.of("U", "B"), 66, new ArrayList<>());

        // Then
        assertThat(result).hasSize(66);
        assertThat(countByPrinting(result, ISLAND_PRINTING)).isEqualTo(4);
        assertThat(countByPrinting(result, SWAMP_PRINTING)).isEqualTo(2);
    }

    @Test
    void shouldSplitEvenlyWhenDeckHasNoColoredPips() {
        // Given
        basicsInCatalog(true, false, true);
        var picked = new ArrayList<>(picked("{3}", 10));

        // When
        var result = padder.pad(picked, Set.of("W", "U"), 12, new ArrayList<>());

        // Then
        assertThat(countByPrinting(result, PLAINS_PRINTING)).isEqualTo(1);
        assertThat(countByPrinting(result, ISLAND_PRINTING)).isEqualTo(1);
    }

    @Test
    void shouldReturnUnchangedWhenNoSlotsMissing() {
        // Given
        var picked = new ArrayList<>(picked("{2}{U}", 5));

        // When
        var result = padder.pad(picked, Set.of("U"), 5, new ArrayList<>());

        // Then
        assertThat(result).hasSize(5);
        verifyNoInteractions(cardCatalogService);
    }

    @Test
    void shouldRecordGapWhenCatalogHasNoBasics() {
        // Given
        var gaps = new ArrayList<String>();
        var picked = new ArrayList<>(picked("{2}{U}", 5));

        // When
        var result = padder.pad(picked, Set.of("U"), 7, gaps);

        // Then
        assertThat(result).hasSize(5);
        assertThat(gaps).containsExactly("2 slots could not be filled from your collection");
    }

    @Test
    void shouldAllocateAllSlotsToAvailableColorWhenOneBasicIsMissing() {
        // Given
        basicsInCatalog(true, false, false);
        var picked = new ArrayList<DeckCandidate>();
        picked.addAll(picked("{2}{U}", 20));
        picked.addAll(picked("{2}{B}", 20));

        // When
        var result = padder.pad(picked, Set.of("U", "B"), 44, new ArrayList<>());

        // Then
        assertThat(countByPrinting(result, ISLAND_PRINTING)).isEqualTo(4);
    }

    private List<Card> basicsInCatalog(boolean island, boolean swamp, boolean plains) {
        var cards = new ArrayList<Card>();
        var printings = new java.util.HashMap<Long, Long>();
        if (island) {
            cards.add(basic(1L, "Island"));
            printings.put(1L, ISLAND_PRINTING);
        }
        if (swamp) {
            cards.add(basic(2L, "Swamp"));
            printings.put(2L, SWAMP_PRINTING);
        }
        if (plains) {
            cards.add(basic(3L, "Plains"));
            printings.put(3L, PLAINS_PRINTING);
        }
        lenient().when(cardCatalogService.getCardsByNames(anyList())).thenReturn(cards);
        lenient()
                .when(cardCatalogService.getLatestPrintingIdByCardIds(anyList()))
                .thenReturn(printings);
        return cards;
    }

    private static Card basic(long id, String name) {
        var card = new Card("oracle-" + name, name);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static List<DeckCandidate> picked(String manaCost, int count) {
        var card = new Card("oracle-picked", "Picked");
        card.setManaCost(manaCost);
        return IntStream.range(0, count)
                .mapToObj(i -> new DeckCandidate(0L, card, Category.SYNERGY, null))
                .toList();
    }

    private static long countByPrinting(List<DeckCandidate> candidates, long printingId) {
        return candidates.stream().filter(c -> c.printingId() == printingId).count();
    }
}
