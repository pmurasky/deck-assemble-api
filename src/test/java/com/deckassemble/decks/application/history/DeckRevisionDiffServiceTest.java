package com.deckassemble.decks.application.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.history.DeckRevisionDiffService.CardChange;
import com.deckassemble.decks.application.history.DeckRevisionDiffService.Diff;
import com.deckassemble.decks.application.history.DeckRevisionDiffService.FieldChange;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckRevisionDiffServiceTest {

    private static final long DECK_ID = 1L;

    @Mock private DeckRevisionService deckRevisionService;

    @Test
    void shouldReportChangedMetadataFieldsOnly() {
        DeckSnapshot from =
                snapshot("Original", "COMMANDER", null, List.of(), List.of(), List.of());
        DeckSnapshot to =
                new DeckSnapshot(
                        "Renamed",
                        "COMMANDER",
                        null,
                        null,
                        null,
                        null,
                        false,
                        new BigDecimal("50"),
                        null,
                        null,
                        "DRAFT",
                        List.of(),
                        List.of(),
                        List.of());

        Diff diff = service().diff(from, to);

        assertThat(diff.metadataChanges())
                .containsExactlyInAnyOrder(
                        new FieldChange("name", "Original", "Renamed"),
                        new FieldChange("budgetLimit", null, "50"));
    }

    @Test
    void shouldReportNoMetadataChangesForIdenticalSnapshots() {
        DeckSnapshot snapshot =
                snapshot("Same", "COMMANDER", null, List.of(), List.of(), List.of());

        Diff diff = service().diff(snapshot, snapshot);

        assertThat(diff.metadataChanges()).isEmpty();
    }

    @Test
    void shouldDiffAddedRemovedAndChangedCards() {
        DeckSnapshot from =
                snapshot(
                        "Deck",
                        "COMMANDER",
                        null,
                        List.of(cardEntry(100L, 2, "MAIN_DECK"), cardEntry(200L, 1, "MAIN_DECK")),
                        List.of(),
                        List.of());
        DeckSnapshot to =
                snapshot(
                        "Deck",
                        "COMMANDER",
                        null,
                        List.of(cardEntry(100L, 3, "MAIN_DECK"), cardEntry(300L, 1, "MAIN_DECK")),
                        List.of(),
                        List.of());

        Diff diff = service().diff(from, to);

        assertThat(diff.cards().added())
                .containsExactly(new CardChange(300L, "MAIN_DECK", null, 1));
        assertThat(diff.cards().removed())
                .containsExactly(new CardChange(200L, "MAIN_DECK", 1, null));
        assertThat(diff.cards().changed()).containsExactly(new CardChange(100L, "MAIN_DECK", 2, 3));
    }

    @Test
    void shouldTreatSamePrintingInDifferentSectionsAsDistinctCards() {
        DeckSnapshot from =
                snapshot(
                        "Deck",
                        "COMMANDER",
                        null,
                        List.of(cardEntry(100L, 1, "MAIN_DECK")),
                        List.of(),
                        List.of());
        DeckSnapshot to =
                snapshot(
                        "Deck",
                        "COMMANDER",
                        null,
                        List.of(cardEntry(100L, 1, "SIDEBOARD")),
                        List.of(),
                        List.of());

        Diff diff = service().diff(from, to);

        assertThat(diff.cards().added())
                .containsExactly(new CardChange(100L, "SIDEBOARD", null, 1));
        assertThat(diff.cards().removed())
                .containsExactly(new CardChange(100L, "MAIN_DECK", 1, null));
        assertThat(diff.cards().changed()).isEmpty();
    }

    @Test
    void shouldDiffCategoryNamesAddedAndRemoved() {
        DeckSnapshot from =
                snapshot(
                        "Deck",
                        "COMMANDER",
                        null,
                        List.of(),
                        List.of("Ramp", "Removal"),
                        List.of());
        DeckSnapshot to =
                snapshot("Deck", "COMMANDER", null, List.of(), List.of("Ramp", "Draw"), List.of());

        Diff diff = service().diff(from, to);

        assertThat(diff.categories().added()).containsExactly("Draw");
        assertThat(diff.categories().removed()).containsExactly("Removal");
    }

    @Test
    void shouldDiffTagNamesAddedAndRemoved() {
        DeckSnapshot from =
                snapshot("Deck", "COMMANDER", null, List.of(), List.of(), List.of("Aggro"));
        DeckSnapshot to =
                snapshot(
                        "Deck", "COMMANDER", null, List.of(), List.of(), List.of("Aggro", "Combo"));

        Diff diff = service().diff(from, to);

        assertThat(diff.tags().added()).containsExactly("Combo");
        assertThat(diff.tags().removed()).isEmpty();
    }

    @Test
    void shouldLoadBothSnapshotsByRevisionNumberBeforeDiffing() {
        DeckSnapshot from = snapshot("From", "COMMANDER", null, List.of(), List.of(), List.of());
        DeckSnapshot to = snapshot("To", "COMMANDER", null, List.of(), List.of(), List.of());
        when(deckRevisionService.snapshotAt(DECK_ID, 1)).thenReturn(from);
        when(deckRevisionService.snapshotAt(DECK_ID, 2)).thenReturn(to);

        Diff diff = service().diff(DECK_ID, 1, 2);

        verify(deckRevisionService).snapshotAt(DECK_ID, 1);
        verify(deckRevisionService).snapshotAt(DECK_ID, 2);
        assertThat(diff.metadataChanges()).containsExactly(new FieldChange("name", "From", "To"));
    }

    private DeckRevisionDiffService service() {
        return new DeckRevisionDiffService(deckRevisionService);
    }

    private static DeckSnapshot.CardEntry cardEntry(long printingId, int quantity, String section) {
        return new DeckSnapshot.CardEntry(printingId, quantity, section, "OWNED");
    }

    private static DeckSnapshot snapshot(
            String name,
            String formatCode,
            String description,
            List<DeckSnapshot.CardEntry> cards,
            List<String> categoryNames,
            List<String> tagNames) {
        return new DeckSnapshot(
                name,
                formatCode,
                description,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                cards,
                categoryNames,
                tagNames);
    }
}
