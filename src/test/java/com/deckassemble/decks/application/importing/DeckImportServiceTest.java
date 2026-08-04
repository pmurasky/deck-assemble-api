package com.deckassemble.decks.application.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.cards.application.CardReferenceResolution;
import com.deckassemble.cards.application.CardReferenceResolver;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckImportPreview;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class DeckImportServiceTest {

    private static final UUID SCRYFALL_ID = UUID.fromString("03fcf7d4-8a1b-4e2f-89f1-12c840e27721");
    @Mock private CardReferenceResolver resolver;
    @Mock private DeckImportPreviewRepository previewRepository;
    @Mock private DeckAccessGuard accessGuard;

    @Test
    void shouldPersistProfileBoundPreview() {
        var reference = new CardReference(null, "Card", "TST", "1");
        when(resolver.resolve(reference)).thenReturn(new CardReferenceResolution.Matched(1L, 101L));
        when(accessGuard.profileId()).thenReturn(7L);

        var result =
                previewService()
                        .preview(
                                "DECKASSEMBLE_TEXT",
                                "1 Card|TST|1".getBytes(StandardCharsets.UTF_8));

        assertThat(result.rows().resolved()).hasSize(1);
        var previewCaptor = ArgumentCaptor.forClass(DeckImportPreview.class);
        verify(previewRepository).save(previewCaptor.capture());
        assertThat(previewCaptor.getValue().getProfileId()).isEqualTo(7L);
        assertThat(previewCaptor.getValue().getCanonicalRows()).contains("\"printingId\":101");
    }

    @ParameterizedTest
    @ValueSource(strings = {"scryfall_id", "Scryfall ID"})
    void shouldParseScryfallIdAndExactTupleFromCsv(String identifierHeader) {
        String source =
                "quantity,name,set,collector_number,section,%s\n".formatted(identifierHeader)
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

    @Test
    void shouldClassifyEveryResolutionAndCalculateTotals() {
        var scryfall = new CardReference(SCRYFALL_ID, "Wrong", "BAD", "0");
        var fallback = new CardReference(UUID.randomUUID(), "Fallback", "SET", "1");
        var ambiguous = new CardReference(null, "Ambiguous", "SET", "2");
        var unmatched = new CardReference(null, "Missing", "SET", "3");
        when(resolver.resolve(scryfall)).thenReturn(new CardReferenceResolution.Matched(1L, 11L));
        when(resolver.resolve(fallback)).thenReturn(new CardReferenceResolution.Matched(2L, 22L));
        when(resolver.resolve(ambiguous))
                .thenReturn(new CardReferenceResolution.Ambiguous(List.of(33L, 34L)));
        when(resolver.resolve(unmatched)).thenReturn(new CardReferenceResolution.Unmatched());

        var rows =
                DeckImportService.PreviewRows.resolve(
                        parsedRows(scryfall, fallback, ambiguous, unmatched), resolver);
        var totals = DeckImportService.Totals.from(rows);

        assertThat(rows.resolved())
                .extracting(DeckImportService.ResolvedRow::printingId)
                .containsExactly(11L, 22L);
        assertThat(rows.ambiguous().getFirst().printingIds()).containsExactly(33L, 34L);
        assertThat(rows.unmatched()).hasSize(1);
        assertThat(rows.invalid()).hasSize(1);
        assertThat(totals).isEqualTo(new DeckImportService.Totals(5, 2, 1, 1, 1));
        verify(resolver).resolve(scryfall);
        verify(resolver).resolve(fallback);
        verify(resolver).resolve(ambiguous);
        verify(resolver).resolve(unmatched);
        verifyNoMoreInteractions(resolver);
    }

    @ParameterizedTest
    @MethodSource("formats")
    void shouldParseQuantitiesAndSectionsForEverySupportedFormat(
            DeckImportParser parser, String fixture) throws IOException {
        String source;
        try (var stream = getClass().getResourceAsStream("/fixtures/deck-imports/" + fixture)) {
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

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

    private DeckImportService previewService() {
        return new DeckImportService(
                List.of(new DeckAssembleTextDeckImportParser()),
                resolver,
                previewRepository,
                accessGuard,
                mapper());
    }

    private static JsonMapper mapper() {
        return JsonMapper.builder().build();
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

    private static List<DeckImportParser.ParsedRow> parsedRows(CardReference... references) {
        var rows =
                Stream.of(references)
                        .map(
                                reference ->
                                        new DeckImportParser.ParsedRow(
                                                1, 1, DeckCard.Section.MAIN_DECK, reference, null))
                        .toList();
        var invalid =
                new DeckImportParser.ParsedRow(
                        2, 0, DeckCard.Section.MAIN_DECK, references[0], "Invalid row");
        return Stream.concat(rows.stream(), Stream.of(invalid)).toList();
    }
}
