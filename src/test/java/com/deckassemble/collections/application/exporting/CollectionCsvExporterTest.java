package com.deckassemble.collections.application.exporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.cards.application.CardExportView;
import com.deckassemble.collections.application.CollectionCardResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CollectionCsvExporterTest {

    private static final String HEADER =
            "scryfall_id,name,set,collector_number,quantity,printing_id\n";

    @Test
    void shouldWriteHeaderAndSummedQuantity() {
        // Given a collection card with regular and foil quantities
        byte[] csv =
                CollectionCsvExporter.export(
                        List.of(card(9L, 5, 2)),
                        Map.of(9L, view(9L, "Alpha Card", null, "tst", "7", "scry-alpha")));

        // When exported, then the header, all six columns, and the summed quantity are written
        assertThat(new String(csv, StandardCharsets.UTF_8))
                .isEqualTo(HEADER + "scry-alpha,Alpha Card,tst,7,7,9\n");
    }

    @Test
    void shouldEscapeCommasQuotesAndNewlinesInNames() {
        // Given a card name containing CSV-reserved characters
        byte[] csv =
                CollectionCsvExporter.export(
                        List.of(card(9L, 1, 0)),
                        Map.of(9L, view(9L, "Delver, \"of Secrets\"\nFoil", null, "t", "7", "s1")));

        // When exported, then the name is quoted with doubled inner quotes
        assertThat(new String(csv, StandardCharsets.UTF_8))
                .isEqualTo(HEADER + "s1,\"Delver, \"\"of Secrets\"\"\nFoil\",t,7,1,9\n");
    }

    @Test
    void shouldPreferFlavorNameForDisplay() {
        // Given a printing with a flavor name
        byte[] csv =
                CollectionCsvExporter.export(
                        List.of(card(9L, 1, 0)),
                        Map.of(9L, view(9L, "Canonical", "Flavor Name", "tst", "7", "scry-1")));

        // When exported, then the flavor name is used as the card name
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains(",Flavor Name,");
    }

    @Test
    void shouldOrderRowsDeterministicallyByNameSetAndCollectorNumber() {
        // Given cards in a shuffled order with case-insensitive name ties
        Map<Long, CardExportView> views =
                Map.of(
                        1L, view(1L, "Beta Card", null, "tst", "2", "scry-beta-2"),
                        2L, view(2L, "alpha card", null, "zzz", "9", "scry-alpha-zzz"),
                        3L, view(3L, "Alpha Card", null, "aaa", "3", "scry-alpha-aaa"),
                        4L, view(4L, "Beta Card", null, "tst", "1", "scry-beta-1"));
        var shuffled = List.of(card(1L, 1, 0), card(3L, 1, 0), card(4L, 1, 0), card(2L, 1, 0));

        // When exported, then rows are sorted by name, then set, then collector number
        byte[] csv = CollectionCsvExporter.export(shuffled, views);

        assertThat(new String(csv, StandardCharsets.UTF_8))
                .isEqualTo(
                        HEADER
                                + "scry-alpha-aaa,Alpha Card,aaa,3,1,3\n"
                                + "scry-alpha-zzz,alpha card,zzz,9,1,2\n"
                                + "scry-beta-1,Beta Card,tst,1,1,4\n"
                                + "scry-beta-2,Beta Card,tst,2,1,1\n");
    }

    @Test
    void shouldRejectMissingPrintingView() {
        // Given a collection card whose printing has no export view
        // When exported, then a specific exception signals the broken reference
        assertThatThrownBy(() -> CollectionCsvExporter.export(List.of(card(9L, 1, 0)), Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Collection references a missing card printing");
    }

    private static CollectionCardResponse card(long printingId, int regular, int foil) {
        return new CollectionCardResponse(null, printingId, regular, foil, null);
    }

    private static CardExportView view(
            long printingId,
            String name,
            String flavorName,
            String setCode,
            String collectorNumber,
            String scryfallId) {
        return new CardExportView(
                printingId,
                name,
                flavorName,
                new CardExportView.PrintingReference(setCode, collectorNumber, scryfallId));
    }
}
