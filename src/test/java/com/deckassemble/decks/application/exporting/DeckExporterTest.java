package com.deckassemble.decks.application.exporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.decks.domain.DeckCard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeckExporterTest {

    @ParameterizedTest
    @MethodSource("formats")
    void shouldExportDeterministicGoldenFile(DeckExporter exporter, String fixture)
            throws IOException {
        String expected;
        try (var stream = getClass().getResourceAsStream("/fixtures/deck-exports/" + fixture)) {
            expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(exporter.export(cards())).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("escapedCsvValues")
    void shouldEscapeCsvQuotesAndNewlines(String value, String expected) {
        assertThat(DeckExporter.csv(value)).isEqualTo(expected);
    }

    private static Stream<Arguments> formats() {
        return Stream.of(
                Arguments.of(new DeckAssembleTextDeckExporter(), "deckassemble.txt"),
                Arguments.of(new GenericCsvDeckExporter(), "generic.csv"),
                Arguments.of(new MoxfieldCsvDeckExporter(), "moxfield.csv"),
                Arguments.of(new ArchidektCsvDeckExporter(), "archidekt.csv"),
                Arguments.of(new ArenaTextDeckExporter(), "arena.txt"),
                Arguments.of(new MtgoTextDeckExporter(), "mtgo.txt"));
    }

    private static Stream<Arguments> escapedCsvValues() {
        return Stream.of(Arguments.of("A\"B", "\"A\"\"B\""), Arguments.of("A\nB", "\"A\nB\""));
    }

    private static List<DeckExporter.ExportCard> cards() {
        return List.of(
                new DeckExporter.ExportCard(
                        DeckCard.Section.MAIN_DECK,
                        1,
                        "Sol Ring",
                        new DeckExporter.PrintingReference(
                                "CMM", "396", "00000000-0000-0000-0000-000000000003")),
                new DeckExporter.ExportCard(
                        DeckCard.Section.MAYBE_BOARD,
                        1,
                        "The Wandering Emperor",
                        new DeckExporter.PrintingReference(
                                "NEO", "42", "00000000-0000-0000-0000-000000000006")),
                new DeckExporter.ExportCard(
                        DeckCard.Section.COMMANDER,
                        1,
                        "Atraxa, Grand Unifier",
                        new DeckExporter.PrintingReference(
                                "MOM", "196", "00000000-0000-0000-0000-000000000001")),
                new DeckExporter.ExportCard(
                        DeckCard.Section.MAIN_DECK,
                        1,
                        "Godzilla, King of the Monsters",
                        new DeckExporter.PrintingReference(
                                "IKO", "275", "00000000-0000-0000-0000-000000000004")),
                new DeckExporter.ExportCard(
                        DeckCard.Section.SIDEBOARD,
                        1,
                        "Negate",
                        new DeckExporter.PrintingReference(
                                "MOM", "68", "00000000-0000-0000-0000-000000000005")),
                new DeckExporter.ExportCard(
                        DeckCard.Section.MAIN_DECK,
                        2,
                        "Arcane Signet",
                        new DeckExporter.PrintingReference(
                                "CMM", "380", "00000000-0000-0000-0000-000000000002")),
                new DeckExporter.ExportCard(
                        DeckCard.Section.COMPANION,
                        1,
                        "Jegantha, the Wellspring",
                        new DeckExporter.PrintingReference(
                                "MUL", "109", "00000000-0000-0000-0000-000000000007")));
    }
}
