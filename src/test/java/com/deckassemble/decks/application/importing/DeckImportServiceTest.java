package com.deckassemble.decks.application.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.decks.domain.DeckCard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeckImportServiceTest {

    @Test
    void shouldParseScryfallIdAndExactTupleFromCsv() {
        String source =
                "quantity,name,set,collector_number,section,scryfall_id\n"
                        + "1,Wrong Name,BAD,0,main,03fcf7d4-8a1b-4e2f-89f1-12c840e27721";

        var reference =
                new GenericCsvDeckImportParser().parse(source).rows().getFirst().reference();

        assertThat(reference.scryfallId())
                .isEqualTo(UUID.fromString("03fcf7d4-8a1b-4e2f-89f1-12c840e27721"));
        assertThat(reference.name()).isEqualTo("Wrong Name");
        assertThat(reference.setCode()).isEqualTo("BAD");
        assertThat(reference.collectorNumber()).isEqualTo("0");
    }

    @ParameterizedTest
    @MethodSource("invalidTextQuantities")
    void shouldReturnInvalidRowForMalformedTextQuantity(
            DeckImportParser parser, String rowTemplate, String quantity) {
        var row = parser.parse(rowTemplate.formatted(quantity)).rows().getFirst();

        assertThat(row.quantity()).isZero();
        assertThat(row.error()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("formats")
    void shouldParseQuantitiesAndSectionsForEverySupportedFormat(
            DeckImportParser parser, String fixture) throws IOException {
        String source =
                new String(
                        getClass()
                                .getResourceAsStream("/fixtures/deck-imports/" + fixture)
                                .readAllBytes(),
                        StandardCharsets.UTF_8);

        var rows = parser.parse(source).rows();

        assertThat(rows)
                .extracting(DeckImportParser.ParsedRow::quantity)
                .containsExactly(1, 2, 1, 1);
        assertThat(rows)
                .extracting(DeckImportParser.ParsedRow::section)
                .containsExactly(
                        DeckCard.Section.COMMANDER,
                        DeckCard.Section.MAIN_DECK,
                        DeckCard.Section.SIDEBOARD,
                        DeckCard.Section.MAYBE_BOARD);
    }

    private static Stream<Arguments> formats() {
        return Stream.of(
                Arguments.of(new DeckAssembleTextDeckImportParser(), "deckassemble.txt"),
                Arguments.of(new GenericCsvDeckImportParser(), "generic.csv"),
                Arguments.of(new MoxfieldCsvDeckImportParser(), "moxfield.csv"),
                Arguments.of(new ArchidektCsvDeckImportParser(), "archidekt.csv"),
                Arguments.of(new ArenaTextDeckImportParser(), "arena.txt"),
                Arguments.of(new MtgoTextDeckImportParser(), "mtgo.txt"));
    }

    private static Stream<Arguments> invalidTextQuantities() {
        return Stream.of(
                Arguments.of(new DeckAssembleTextDeckImportParser(), "%s Card|SET|1", "2147483648"),
                Arguments.of(new DeckAssembleTextDeckImportParser(), "%s Card|SET|1", "invalid"),
                Arguments.of(new ArenaTextDeckImportParser(), "%s Card (SET) 1", "2147483648"),
                Arguments.of(new ArenaTextDeckImportParser(), "%s Card (SET) 1", "invalid"),
                Arguments.of(new MtgoTextDeckImportParser(), "%s Card [SET:1]", "2147483648"),
                Arguments.of(new MtgoTextDeckImportParser(), "%s Card [SET:1]", "invalid"));
    }
}
