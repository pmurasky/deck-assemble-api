package com.deckassemble.decks.application.exporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.decks.application.importing.ArchidektCsvDeckImportParser;
import com.deckassemble.decks.application.importing.ArenaTextDeckImportParser;
import com.deckassemble.decks.application.importing.DeckAssembleTextDeckImportParser;
import com.deckassemble.decks.application.importing.DeckImportParser;
import com.deckassemble.decks.application.importing.GenericCsvDeckImportParser;
import com.deckassemble.decks.application.importing.MoxfieldCsvDeckImportParser;
import com.deckassemble.decks.application.importing.MtgoTextDeckImportParser;
import com.deckassemble.decks.domain.DeckCard;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeckExportRoundTripTest {

    @ParameterizedTest
    @MethodSource("formats")
    void shouldRoundTripCompanionSection(DeckExporter exporter, DeckImportParser parser) {
        var companion = card(DeckCard.Section.COMPANION, "Jegantha, the Wellspring", "MUL", "109");

        var row = parser.parse(exporter.export(List.of(companion))).rows().getFirst();

        assertThat(row.section()).isEqualTo(DeckCard.Section.COMPANION);
    }

    @ParameterizedTest
    @MethodSource("textFormats")
    void shouldRoundTripFlavorNameAndExactPrintingTuple(
            DeckExporter exporter, DeckImportParser parser) {
        var flavorCard =
                card(DeckCard.Section.MAIN_DECK, "Godzilla, King of the Monsters", "IKO", "275");

        var reference =
                parser.parse(exporter.export(List.of(flavorCard))).rows().getFirst().reference();

        assertThat(reference.name()).isEqualTo("Godzilla, King of the Monsters");
        assertThat(reference.setCode()).isEqualTo("IKO");
        assertThat(reference.collectorNumber()).isEqualTo("275");
    }

    private static DeckExporter.ExportCard card(
            DeckCard.Section section, String name, String setCode, String collectorNumber) {
        return new DeckExporter.ExportCard(
                section,
                1,
                name,
                new DeckExporter.PrintingReference(
                        setCode, collectorNumber, "00000000-0000-0000-0000-000000000099"));
    }

    private static Stream<Arguments> formats() {
        return Stream.of(
                Arguments.of(
                        new DeckAssembleTextDeckExporter(), new DeckAssembleTextDeckImportParser()),
                Arguments.of(new GenericCsvDeckExporter(), new GenericCsvDeckImportParser()),
                Arguments.of(new MoxfieldCsvDeckExporter(), new MoxfieldCsvDeckImportParser()),
                Arguments.of(new ArchidektCsvDeckExporter(), new ArchidektCsvDeckImportParser()),
                Arguments.of(new ArenaTextDeckExporter(), new ArenaTextDeckImportParser()),
                Arguments.of(new MtgoTextDeckExporter(), new MtgoTextDeckImportParser()));
    }

    private static Stream<Arguments> textFormats() {
        return Stream.of(
                Arguments.of(
                        new DeckAssembleTextDeckExporter(), new DeckAssembleTextDeckImportParser()),
                Arguments.of(new ArenaTextDeckExporter(), new ArenaTextDeckImportParser()),
                Arguments.of(new MtgoTextDeckExporter(), new MtgoTextDeckImportParser()));
    }
}
