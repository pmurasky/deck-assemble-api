package com.deckassemble.decks.application.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckImportPreview;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class DeckImportCommitServiceTest {

    @Mock private DeckImportPreviewRepository previewRepository;
    @Mock private DeckAccessGuard accessGuard;
    @Mock private DeckService deckService;
    @Mock private DeckCardService deckCardService;
    @Mock private DeckRevisionService deckRevisionService;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void shouldCommitSelectedCardsAndReturnRefreshedDeck() {
        var rows =
                new DeckImportService.PreviewRows(
                        List.of(resolvedRow(1, 2, 101L), resolvedRow(2, 1, 102L)),
                        List.of(),
                        List.of(new DeckImportService.UnmatchedRow(row(3, 1))),
                        List.of());
        var preview = pendingPreview(rows, Instant.now().plusSeconds(60));
        preparePending(preview, "key");
        when(deckService.create(any())).thenReturn(deckResponse(10L, "Deck", 0));
        when(deckService.getById(10L)).thenReturn(deckResponse(10L, "Deck", 2));
        var requestCaptor = ArgumentCaptor.forClass(DeckCardAddRequest.class);

        var result = service().commit(preview.getToken(), "Deck", Set.of(2, 3), "key");

        assertThat(result.deck().cardCount()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
        verify(deckCardService).addCard(eq(10L), requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .isEqualTo(new DeckCardAddRequest(101L, 2, DeckCard.Section.MAIN_DECK, null));
        assertThat(preview.getStatus()).isEqualTo(DeckImportPreview.Status.COMMITTED);
        assertThat(preview.getIdempotencyKey()).isEqualTo("key");
        assertThat(preview.getCommittedDeckId()).isEqualTo(10L);
        var snapshot =
                mapper.readValue(
                        preview.getCanonicalRows(), DeckImportCommitService.CommitSnapshot.class);
        assertThat(snapshot.imported()).isEqualTo(1);
        assertThat(snapshot.skipped()).isEqualTo(2);
        verify(deckRevisionService).withoutRecording(any());
        verify(deckRevisionService).record(10L, 1L, DeckChangeType.IMPORTED);
        verify(deckRevisionService, times(1)).record(anyLong(), anyLong(), any());
    }

    @Test
    void shouldReturnPersistedResultForAlteredRetryBody() {
        var preview = committedPreview(rows(resolvedRow(1, 1, 101L)), 10L, "key", 1, 0);
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 1L))
                .thenReturn(Optional.of(preview));

        var result = service().commit(preview.getToken(), "Changed", Set.of(1, 999), "key");

        assertThat(result.deck().name()).isEqualTo("Original");
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        verify(previewRepository, never()).findByProfileIdAndIdempotencyKey(1L, "key");
        verifyNoInteractions(deckCardService);
    }

    @Test
    void shouldReplayExistingKeyAfterValidatingDifferentPendingPreview() {
        var supplied = pendingPreview(rows(resolvedRow(1, 1, 101L)), Instant.now().plusSeconds(60));
        var original = committedPreview(rows(resolvedRow(2, 1, 102L)), 10L, "key", 1, 0);
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(supplied.getToken(), 1L))
                .thenReturn(Optional.of(supplied));
        when(previewRepository.findByProfileIdAndIdempotencyKey(1L, "key"))
                .thenReturn(Optional.of(original));

        var result = service().commit(supplied.getToken(), "Second", Set.of(), "key");

        assertThat(result.deck().name()).isEqualTo("Original");
        assertThat(supplied.getStatus()).isEqualTo(DeckImportPreview.Status.PENDING);
        verifyNoInteractions(deckCardService);
    }

    @Test
    void shouldRejectForeignPreviewBeforeLookingUpExistingKey() {
        var token = UUID.randomUUID();
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(token, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().commit(token, "Deck", Set.of(), "key"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(previewRepository, never()).findByProfileIdAndIdempotencyKey(1L, "key");
        verifyNoInteractions(deckService, deckCardService);
    }

    @Test
    void shouldRejectExpiredPreviewBeforeLookingUpExistingKey() {
        var preview = pendingPreview(rows(resolvedRow(1, 1, 101L)), Instant.now().minusSeconds(1));
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 1L))
                .thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service().commit(preview.getToken(), "Deck", Set.of(), "key"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(previewRepository, never()).findByProfileIdAndIdempotencyKey(1L, "key");
        verifyNoInteractions(deckService, deckCardService);
    }

    @Test
    void shouldRejectCommittedPreviewWithDifferentKey() {
        var preview = committedPreview(rows(resolvedRow(1, 1, 101L)), 10L, "original", 1, 0);
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 1L))
                .thenReturn(Optional.of(preview));

        assertThatThrownBy(
                        () -> service().commit(preview.getToken(), "Deck", Set.of(), "different"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verifyNoInteractions(deckService, deckCardService);
    }

    @Test
    void shouldRejectCommittedPreviewWithoutResultingDeck() {
        var rows = rows(resolvedRow(1, 1, 101L));
        var preview = pendingPreview(rows, Instant.now().plusSeconds(60));
        preview.storeCanonicalRows(
                mapper.writeValueAsString(
                        new DeckImportCommitService.CommitSnapshot(
                                rows, deckResponse(10L, "Deck", 1), 1, 0)));
        preview.markCommitted("key", null);
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 1L))
                .thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service().commit(preview.getToken(), "Deck", Set.of(), "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Committed import has no deck");
        verifyNoInteractions(deckService, deckCardService);
    }

    @Test
    void shouldLeavePreviewPendingWhenCardAdditionFails() {
        var preview = pendingPreview(rows(resolvedRow(1, 1, 101L)), Instant.now().plusSeconds(60));
        String canonicalRows = preview.getCanonicalRows();
        preparePending(preview, "key");
        when(deckService.create(any())).thenReturn(deckResponse(10L, "Deck", 0));
        when(deckCardService.addCard(eq(10L), any()))
                .thenThrow(new IllegalStateException("card write failed"));

        assertThatThrownBy(() -> service().commit(preview.getToken(), "Deck", Set.of(), "key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("card write failed");
        assertThat(preview.getStatus()).isEqualTo(DeckImportPreview.Status.PENDING);
        assertThat(preview.getCanonicalRows()).isEqualTo(canonicalRows);
        verify(previewRepository, never()).save(preview);
        verify(deckService, never()).getById(10L);
        verify(deckRevisionService, never()).record(anyLong(), anyLong(), any());
    }

    @Test
    void shouldBlockCommitWhileUnresolvedRowsRemain() {
        var rows =
                new DeckImportService.PreviewRows(
                        List.of(),
                        List.of(),
                        List.of(new DeckImportService.UnmatchedRow(row(1, 1))),
                        List.of());
        var preview = pendingPreview(rows, Instant.now().plusSeconds(60));
        preparePending(preview, "key");

        assertThatThrownBy(() -> service().commit(preview.getToken(), "Deck", Set.of(), "key"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unresolved import rows");
        verifyNoInteractions(deckService, deckCardService);
    }

    private DeckImportCommitService service() {
        return new DeckImportCommitService(
                previewRepository,
                accessGuard,
                mapper,
                deckService,
                deckCardService,
                deckRevisionService);
    }

    private void preparePending(DeckImportPreview preview, String idempotencyKey) {
        when(accessGuard.lockedProfileId()).thenReturn(1L);
        when(previewRepository.findLockedByTokenAndProfileId(preview.getToken(), 1L))
                .thenReturn(Optional.of(preview));
        when(previewRepository.findByProfileIdAndIdempotencyKey(1L, idempotencyKey))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .when(
                        deckRevisionService.withoutRecording(
                                org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private DeckImportPreview pendingPreview(
            DeckImportService.PreviewRows rows, Instant expiresAt) {
        return new DeckImportPreview(
                UUID.randomUUID(), 1L, expiresAt, "a".repeat(64), mapper.writeValueAsString(rows));
    }

    private DeckImportPreview committedPreview(
            DeckImportService.PreviewRows rows,
            long deckId,
            String idempotencyKey,
            int imported,
            int skipped) {
        var preview = pendingPreview(rows, Instant.now().plusSeconds(60));
        preview.storeCanonicalRows(
                mapper.writeValueAsString(
                        new DeckImportCommitService.CommitSnapshot(
                                rows,
                                deckResponse(deckId, "Original", imported),
                                imported,
                                skipped)));
        preview.markCommitted(idempotencyKey, deckId);
        return preview;
    }

    private static DeckImportService.PreviewRows rows(DeckImportService.ResolvedRow... resolved) {
        return new DeckImportService.PreviewRows(
                List.of(resolved), List.of(), List.of(), List.of());
    }

    private static DeckImportService.ResolvedRow resolvedRow(
            int lineNumber, int quantity, long printingId) {
        return new DeckImportService.ResolvedRow(
                row(lineNumber, quantity), printingId + 1000, printingId);
    }

    private static DeckImportService.Row row(int lineNumber, int quantity) {
        return new DeckImportService.Row(
                lineNumber,
                quantity,
                DeckCard.Section.MAIN_DECK,
                new CardReference(null, "Card", "TST", String.valueOf(lineNumber)));
    }

    private static DeckResponse deckResponse(long id, String name, int cardCount) {
        return new DeckResponse(
                id,
                name,
                "COMMANDER",
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                cardCount,
                null,
                null,
                Instant.parse("2026-08-04T00:00:00Z"),
                0);
    }
}
