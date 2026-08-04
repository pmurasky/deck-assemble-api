package com.deckassemble.decks.application.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.decks.domain.DeckCard;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeckImportServiceTest {

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
}
