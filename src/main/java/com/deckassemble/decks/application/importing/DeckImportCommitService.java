package com.deckassemble.decks.application.importing;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.DeckImportPreview;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/**
 * Atomically creates decks from validated import previews. Composes the already-instrumented {@link
 * DeckService#create} and {@link DeckCardService#addCard}, but records exactly one {@code IMPORTED}
 * revision for the whole operation rather than one per composed call: those calls run inside {@link
 * DeckRevisionService#withoutRecording}, then a single revision is recorded here.
 */
@Service
public class DeckImportCommitService {

    private final DeckImportPreviewRepository previewRepository;
    private final DeckAccessGuard accessGuard;
    private final ObjectMapper objectMapper;
    private final DeckService deckService;
    private final DeckCardService deckCardService;
    private final DeckRevisionService deckRevisionService;

    // Suppressed: cohesive import-commit collaborators (preview persistence, ownership, JSON
    // codec, deck/card mutation, and history recording); no natural subgrouping without an
    // artificial wrapper, same precedent as DeckController.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckImportCommitService(
            DeckImportPreviewRepository previewRepository,
            DeckAccessGuard accessGuard,
            ObjectMapper objectMapper,
            DeckService deckService,
            DeckCardService deckCardService,
            DeckRevisionService deckRevisionService) {
        this.previewRepository = previewRepository;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.deckRevisionService = deckRevisionService;
    }

    @Transactional
    public CommitResult commit(
            UUID token, String name, Set<Integer> excludedLineNumbers, String idempotencyKey) {
        long profileId = accessGuard.lockedProfileId();
        DeckImportPreview preview = ownedPreview(token, profileId);
        if (preview.getStatus() != DeckImportPreview.Status.PENDING) {
            return replayCommitted(preview, idempotencyKey);
        }
        var replay = previewRepository.findByProfileIdAndIdempotencyKey(profileId, idempotencyKey);
        if (replay.isPresent()) {
            return resultFor(replay.orElseThrow());
        }
        var rows = rowsFrom(preview);
        rejectUnresolved(rows, excludedLineNumbers);
        return commitPreview(preview, rows, name, excludedLineNumbers, idempotencyKey);
    }

    private CommitResult commitPreview(
            DeckImportPreview preview,
            DeckImportService.PreviewRows rows,
            String name,
            Set<Integer> excluded,
            String idempotencyKey) {
        // The deck create + per-row card adds below are already-instrumented mutations, but a
        // whole import commits as one meaningful change, not one revision per composed call.
        CommitOutcome outcome =
                deckRevisionService.withoutRecording(
                        () -> {
                            DeckResponse created = deckService.create(deckRequest(name));
                            int imported = addSelected(created.id(), rows.resolved(), excluded);
                            return new CommitOutcome(created, imported);
                        });
        int skipped = excludedCount(rows, excluded);
        DeckResponse refreshed = deckService.getById(outcome.deck().id());
        deckRevisionService.record(
                outcome.deck().id(), preview.getProfileId(), DeckChangeType.IMPORTED);
        preview.storeCanonicalRows(
                objectMapper.writeValueAsString(
                        new CommitSnapshot(rows, refreshed, outcome.imported(), skipped)));
        preview.markCommitted(idempotencyKey, outcome.deck().id());
        previewRepository.save(preview);
        return new CommitResult(refreshed, outcome.imported(), skipped);
    }

    private record CommitOutcome(DeckResponse deck, int imported) {}

    private DeckImportPreview ownedPreview(UUID token, long profileId) {
        DeckImportPreview preview =
                previewRepository
                        .findLockedByTokenAndProfileId(token, profileId)
                        .orElseThrow(() -> notFound("Import preview not found"));
        if (preview.getExpiresAt().isBefore(Instant.now())) {
            throw notFound("Import preview not found");
        }
        return preview;
    }

    private DeckImportService.PreviewRows rowsFrom(DeckImportPreview preview) {
        return objectMapper.readValue(
                preview.getCanonicalRows(), DeckImportService.PreviewRows.class);
    }

    private void rejectUnresolved(DeckImportService.PreviewRows rows, Set<Integer> excluded) {
        var unresolved =
                java.util.stream.Stream.of(
                                rows.ambiguous().stream().map(DeckImportService.AmbiguousRow::row),
                                rows.unmatched().stream().map(DeckImportService.UnmatchedRow::row),
                                rows.invalid().stream().map(DeckImportService.InvalidRow::row))
                        .flatMap(java.util.function.Function.identity())
                        .filter(row -> !excluded.contains(row.lineNumber()))
                        .findAny();
        if (unresolved.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unresolved import rows must be excluded");
        }
    }

    private int addSelected(
            long deckId, List<DeckImportService.ResolvedRow> rows, Set<Integer> excluded) {
        var selected =
                rows.stream().filter(row -> !excluded.contains(row.row().lineNumber())).toList();
        selected.forEach(
                row ->
                        deckCardService.addCard(
                                deckId,
                                new DeckCardAddRequest(
                                        row.printingId(),
                                        row.row().quantity(),
                                        row.row().section(),
                                        null)));
        return selected.size();
    }

    private CommitResult resultFor(DeckImportPreview preview) {
        if (preview.getCommittedDeckId() == null) {
            throw new IllegalStateException("Committed import has no deck");
        }
        CommitSnapshot snapshot = snapshotFrom(preview);
        return new CommitResult(snapshot.deck(), snapshot.imported(), snapshot.skipped());
    }

    private CommitResult replayCommitted(DeckImportPreview preview, String idempotencyKey) {
        if (!Objects.equals(preview.getIdempotencyKey(), idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Import preview is committed");
        }
        return resultFor(preview);
    }

    private CommitSnapshot snapshotFrom(DeckImportPreview preview) {
        return objectMapper.readValue(preview.getCanonicalRows(), CommitSnapshot.class);
    }

    private static int excludedCount(DeckImportService.PreviewRows rows, Set<Integer> excluded) {
        return (int)
                java.util.stream.Stream.of(
                                rows.resolved().stream().map(DeckImportService.ResolvedRow::row),
                                rows.ambiguous().stream().map(DeckImportService.AmbiguousRow::row),
                                rows.unmatched().stream().map(DeckImportService.UnmatchedRow::row),
                                rows.invalid().stream().map(DeckImportService.InvalidRow::row))
                        .flatMap(java.util.function.Function.identity())
                        .filter(row -> excluded.contains(row.lineNumber()))
                        .count();
    }

    private static DeckCreateRequest deckRequest(String name) {
        return new DeckCreateRequest(name, "COMMANDER", null, null, null, false, null, null, null);
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }

    public record CommitResult(DeckResponse deck, int imported, int skipped) {}

    public record CommitSnapshot(
            DeckImportService.PreviewRows rows, DeckResponse deck, int imported, int skipped) {}
}
