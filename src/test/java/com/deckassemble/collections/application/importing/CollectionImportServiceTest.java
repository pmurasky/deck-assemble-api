package com.deckassemble.collections.application.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.cards.application.CardReferenceResolution;
import com.deckassemble.cards.application.CardReferenceResolver;
import com.deckassemble.collections.api.importing.CollectionImportPreset;
import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.application.CollectionCardAddRequest;
import com.deckassemble.collections.application.CollectionCreateRequest;
import com.deckassemble.collections.application.CollectionResponse;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.collections.domain.CollectionImportPreview;
import com.deckassemble.collections.domain.CollectionImportPreviewRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CollectionImportServiceTest {

    private static final UUID SCRYFALL_ID = UUID.fromString("03fcf7d4-8a1b-4e2f-89f1-12c840e27721");
    private static final CardReference ATRAXA =
            new CardReference(null, "Atraxa, Praetors' Voice", "2X2", "170");

    @Mock private CardReferenceResolver resolver;
    @Mock private CollectionImportPreviewRepository previewRepository;
    @Mock private CollectionService collectionService;
    @Mock private CollectionAccessGuard accessGuard;

    @ParameterizedTest
    @MethodSource("presets")
    void shouldParseQuantitiesAndReferencesForEveryPreset(
            CollectionImportPreset preset, String fixture) throws IOException {
        var rows = CollectionCsvParser.parse(fixture(fixture), preset.defaultMapping().toLayout());

        assertThat(rows)
                .extracting(CollectionCsvParser.ParsedRow::quantity)
                .containsExactly(2, 1, 3, 0, 1, 1);
        assertThat(rows)
                .extracting(CollectionCsvParser.ParsedRow::lineNumber)
                .containsExactly(2, 3, 4, 5, 6, 7);
        assertThat(rows.get(3).error()).isEqualTo("Invalid quantity 'abc'");
        assertThat(rows.getFirst().reference()).isEqualTo(ATRAXA);
        assertThat(rows.get(4).reference().name()).isEqualTo("Lim-Dûl the Necromancer");
        assertThat(rows.get(5).reference().setCode()).isEqualTo("XXX");
    }

    @ParameterizedTest
    @MethodSource("scryfallPresets")
    void shouldParseScryfallIdOnlyWherePresetMapsIt(
            CollectionImportPreset preset, String fixture, UUID expected) throws IOException {
        var rows = CollectionCsvParser.parse(fixture(fixture), preset.defaultMapping().toLayout());

        assertThat(rows.get(1).reference().scryfallId()).isEqualTo(expected);
    }

    @Test
    void shouldAggregateDuplicateExactPrintings() {
        when(resolver.resolve(ATRAXA)).thenReturn(new CardReferenceResolution.Matched(1L, 101L));
        var parsed =
                List.of(
                        new CollectionCsvParser.ParsedRow(2, 2, ATRAXA, null),
                        new CollectionCsvParser.ParsedRow(4, 3, ATRAXA, null));

        var resolved = CollectionImportService.PreviewRows.resolve(parsed, resolver).resolved();

        assertThat(resolved).hasSize(1);
        assertThat(resolved.getFirst().printingId()).isEqualTo(101L);
        assertThat(resolved.getFirst().row().quantity()).isEqualTo(5);
        assertThat(resolved.getFirst().row().lineNumber()).isEqualTo(2);
    }

    @Test
    void shouldClassifyEveryResolutionAndCalculateTotals() {
        var matched = new CardReference(SCRYFALL_ID, "Wrong", "BAD", "0");
        var ambiguous = new CardReference(null, "Ambiguous", null, null);
        var unmatched = new CardReference(null, "Missing", "SET", "3");
        when(resolver.resolve(matched)).thenReturn(new CardReferenceResolution.Matched(1L, 11L));
        when(resolver.resolve(ambiguous))
                .thenReturn(new CardReferenceResolution.Ambiguous(List.of(33L, 34L)));
        when(resolver.resolve(unmatched)).thenReturn(new CardReferenceResolution.Unmatched());
        var parsed =
                List.of(
                        new CollectionCsvParser.ParsedRow(2, 1, matched, null),
                        new CollectionCsvParser.ParsedRow(3, 1, ambiguous, null),
                        new CollectionCsvParser.ParsedRow(4, 1, unmatched, null),
                        new CollectionCsvParser.ParsedRow(5, 0, unmatched, "Invalid row"));

        var rows = CollectionImportService.PreviewRows.resolve(parsed, resolver);
        var totals = CollectionImportService.Totals.from(rows);

        assertThat(rows.resolved()).extracting("printingId").containsExactly(11L);
        assertThat(rows.ambiguous().getFirst().printingIds()).containsExactly(33L, 34L);
        assertThat(rows.unmatched()).hasSize(1);
        assertThat(rows.invalid()).hasSize(1);
        assertThat(totals).isEqualTo(new CollectionImportService.Totals(4, 1, 1, 1, 1));
    }

    @Test
    void shouldPersistProfileBoundPreview() throws IOException {
        var solRing = new CardReference(SCRYFALL_ID, "Sol Ring", "CMM", "396");
        var limDul = new CardReference(null, "Lim-Dûl the Necromancer", "ICE", "10");
        var nonexistent = new CardReference(null, "Nonexistent Card", "XXX", "999");
        when(resolver.resolve(ATRAXA)).thenReturn(new CardReferenceResolution.Matched(1L, 101L));
        when(resolver.resolve(solRing)).thenReturn(new CardReferenceResolution.Matched(2L, 102L));
        when(resolver.resolve(limDul)).thenReturn(new CardReferenceResolution.Unmatched());
        when(resolver.resolve(nonexistent)).thenReturn(new CardReferenceResolution.Unmatched());
        when(accessGuard.profileId()).thenReturn(7L);
        byte[] source = fixture("deckassemble.csv").getBytes(StandardCharsets.UTF_8);

        var result =
                service()
                        .preview(
                                CollectionImportPreset.DECKASSEMBLE.defaultMapping().toLayout(),
                                source);

        assertThat(result.rows().resolved()).extracting("printingId").containsExactly(101L, 102L);
        assertThat(result.rows().resolved().getFirst().row().quantity()).isEqualTo(5);
        assertThat(result.totals()).isEqualTo(new CollectionImportService.Totals(5, 2, 0, 2, 1));
        var captor = ArgumentCaptor.forClass(CollectionImportPreview.class);
        verify(previewRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getProfileId()).isEqualTo(7L);
        assertThat(saved.getToken()).isEqualTo(result.token());
        assertThat(saved.getSourceSha256()).hasSize(64);
        assertThat(saved.getCanonicalRows()).contains("\"printingId\":101");
        assertThat(Duration.between(Instant.now(), saved.getExpiresAt()).toMinutes())
                .isBetween(29L, 30L);
    }

    @Test
    void shouldRejectOversizedFile() {
        byte[] oversized = new byte[CollectionImportService.MAX_FILE_BYTES + 1];

        assertThatThrownBy(
                        () ->
                                service()
                                        .preview(
                                                CollectionImportPreset.GENERIC
                                                        .defaultMapping()
                                                        .toLayout(),
                                                oversized))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void shouldRejectTooManyRows() {
        String source = "quantity,name\n" + "1,Card\n".repeat(CollectionImportService.MAX_ROWS + 1);

        assertThatThrownBy(
                        () ->
                                service()
                                        .preview(
                                                CollectionImportPreset.GENERIC
                                                        .defaultMapping()
                                                        .toLayout(),
                                                source.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void shouldCommitSelectedRowsAndStoreSnapshot() {
        var rows = previewRows();
        var preview = pendingPreview(rows);
        when(accessGuard.lockedProfileId()).thenReturn(7L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 7L))
                .thenReturn(Optional.of(preview));
        when(previewRepository.findByProfileIdAndIdempotencyKey(7L, "commit-key"))
                .thenReturn(Optional.empty());
        when(collectionService.create(any(CollectionCreateRequest.class)))
                .thenReturn(new CollectionResponse(55L, "Imported", "", false, Instant.now()));

        var result = service().commit(preview.getToken(), "Imported", Set.of(4), "commit-key");

        assertThat(result.collection().id()).isEqualTo(55L);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(collectionService).addCard(55L, new CollectionCardAddRequest(101L, 5, 0));
        assertThat(preview.getStatus()).isEqualTo(CollectionImportPreview.Status.COMMITTED);
        assertThat(preview.getIdempotencyKey()).isEqualTo("commit-key");
        assertThat(preview.getCommittedCollectionId()).isEqualTo(55L);
        assertThat(preview.getCanonicalRows()).contains("\"imported\":1");
    }

    @Test
    void shouldRejectCommitWhenUnresolvedRowsAreNotExcluded() {
        var preview = pendingPreview(previewRows());
        when(accessGuard.lockedProfileId()).thenReturn(7L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 7L))
                .thenReturn(Optional.of(preview));
        when(previewRepository.findByProfileIdAndIdempotencyKey(7L, "blocked-key"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service()
                                        .commit(
                                                preview.getToken(),
                                                "Blocked",
                                                Set.of(),
                                                "blocked-key"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
        verify(collectionService, never()).create(any());
    }

    @Test
    void shouldReplayCommittedResultForSameIdempotencyKey() {
        var rows = previewRows();
        var preview = pendingPreview(rows);
        preview.storeCanonicalRows(
                mapper().writeValueAsString(
                                new CollectionImportService.CommitSnapshot(
                                        rows, collection(55L), 1, 1)));
        preview.markCommitted("commit-key", 55L);
        when(accessGuard.lockedProfileId()).thenReturn(7L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 7L))
                .thenReturn(Optional.of(preview));

        var result = service().commit(preview.getToken(), "Changed Retry", Set.of(), "commit-key");

        assertThat(result.collection().id()).isEqualTo(55L);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(collectionService, never()).create(any());
    }

    @Test
    void shouldRejectCommittedPreviewWithDifferentIdempotencyKey() {
        var preview = pendingPreview(previewRows());
        preview.markCommitted("original-key", 55L);
        when(accessGuard.lockedProfileId()).thenReturn(7L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 7L))
                .thenReturn(Optional.of(preview));

        assertThatThrownBy(
                        () -> service().commit(preview.getToken(), "Retry", Set.of(), "other-key"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void shouldHideForeignAndExpiredPreviews() {
        var token = UUID.randomUUID();
        when(accessGuard.lockedProfileId()).thenReturn(7L);
        when(previewRepository.findLockedByTokenAndProfileId(token, 7L))
                .thenReturn(Optional.empty());
        var expired =
                new CollectionImportPreview(
                        UUID.randomUUID(),
                        7L,
                        Instant.now().minusSeconds(1),
                        "sha",
                        mapper().writeValueAsString(previewRows()));
        when(previewRepository.findLockedByTokenAndProfileId(expired.getToken(), 7L))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service().commit(token, "Foreign", Set.of(), "key"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service().commit(expired.getToken(), "Expired", Set.of(), "key"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void shouldExportRejectedRowsWithReasonCodes() {
        var rows =
                new CollectionImportService.PreviewRows(
                        List.of(),
                        List.of(
                                new CollectionImportService.AmbiguousRow(
                                        new CollectionImportService.Row(3, 1, ATRAXA),
                                        List.of(11L, 12L))),
                        List.of(
                                new CollectionImportService.UnmatchedRow(
                                        new CollectionImportService.Row(
                                                4,
                                                1,
                                                new CardReference(null, "Ghost", "GHO", "9")))),
                        List.of(
                                new CollectionImportService.InvalidRow(
                                        new CollectionImportService.Row(
                                                5,
                                                0,
                                                new CardReference(null, "Broken", "BRK", "2")),
                                        "Invalid quantity 'abc'")));

        String csv = new String(CollectionImportErrorExporter.export(rows), StandardCharsets.UTF_8);

        assertThat(csv.lines().toList())
                .containsExactly(
                        "line_number,reason,quantity,name,set_code,collector_number,scryfall_id,detail",
                        "3,AMBIGUOUS,1,\"Atraxa, Praetors' Voice\",2X2,170,,11;12",
                        "4,UNMATCHED,1,Ghost,GHO,9,,",
                        "5,INVALID,0,Broken,BRK,2,,Invalid quantity 'abc'");
    }

    @Test
    void shouldExportErrorsForPendingAndCommittedPreviews() {
        var rows = previewRows();
        var pending = pendingPreview(rows);
        var committed = pendingPreview(rows);
        committed.storeCanonicalRows(
                mapper().writeValueAsString(
                                new CollectionImportService.CommitSnapshot(
                                        rows, collection(55L), 1, 1)));
        committed.markCommitted("commit-key", 55L);
        when(accessGuard.profileId()).thenReturn(7L);
        when(previewRepository.findByTokenAndProfileId(pending.getToken(), 7L))
                .thenReturn(Optional.of(pending));
        when(previewRepository.findByTokenAndProfileId(committed.getToken(), 7L))
                .thenReturn(Optional.of(committed));

        var pendingCsv = new String(service().errors(pending.getToken()), StandardCharsets.UTF_8);
        var committedCsv =
                new String(service().errors(committed.getToken()), StandardCharsets.UTF_8);

        assertThat(pendingCsv).isEqualTo(committedCsv);
        assertThat(pendingCsv).contains("4,UNMATCHED,1,Ghost,GHO,9,,");
    }

    private CollectionImportService service() {
        return new CollectionImportService(
                resolver, previewRepository, collectionService, accessGuard, mapper());
    }

    private CollectionImportPreview pendingPreview(CollectionImportService.PreviewRows rows) {
        return new CollectionImportPreview(
                UUID.randomUUID(),
                7L,
                Instant.now().plus(Duration.ofMinutes(30)),
                "sha",
                mapper().writeValueAsString(rows));
    }

    private static CollectionImportService.PreviewRows previewRows() {
        return new CollectionImportService.PreviewRows(
                List.of(
                        new CollectionImportService.ResolvedRow(
                                new CollectionImportService.Row(2, 5, ATRAXA), 101L)),
                List.of(),
                List.of(
                        new CollectionImportService.UnmatchedRow(
                                new CollectionImportService.Row(
                                        4, 1, new CardReference(null, "Ghost", "GHO", "9")))),
                List.of());
    }

    private static CollectionResponse collection(long id) {
        return new CollectionResponse(id, "Imported", "", false, Instant.now());
    }

    private static JsonMapper mapper() {
        return JsonMapper.builder().build();
    }

    private static String fixture(String name) throws IOException {
        try (var stream =
                CollectionImportServiceTest.class.getResourceAsStream(
                        "/fixtures/collection-imports/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Stream<Arguments> presets() {
        return Stream.of(
                Arguments.of(CollectionImportPreset.DECKASSEMBLE, "deckassemble.csv"),
                Arguments.of(CollectionImportPreset.MOXFIELD, "moxfield.csv"),
                Arguments.of(CollectionImportPreset.ARCHIDEKT, "archidekt.csv"),
                Arguments.of(CollectionImportPreset.MANABOX, "manabox.csv"),
                Arguments.of(CollectionImportPreset.GENERIC, "generic.csv"));
    }

    private static Stream<Arguments> scryfallPresets() {
        return Stream.of(
                Arguments.of(CollectionImportPreset.DECKASSEMBLE, "deckassemble.csv", SCRYFALL_ID),
                Arguments.of(CollectionImportPreset.MOXFIELD, "moxfield.csv", null),
                Arguments.of(CollectionImportPreset.ARCHIDEKT, "archidekt.csv", null),
                Arguments.of(CollectionImportPreset.MANABOX, "manabox.csv", SCRYFALL_ID),
                Arguments.of(CollectionImportPreset.GENERIC, "generic.csv", SCRYFALL_ID));
    }
}
