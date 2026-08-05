package com.deckassemble.collections.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.cards.application.CardReferenceResolution;
import com.deckassemble.cards.application.CardReferenceResolver;
import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.application.CollectionCardAddRequest;
import com.deckassemble.collections.application.CollectionCreateRequest;
import com.deckassemble.collections.application.CollectionResponse;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.collections.domain.CollectionImportPreview;
import com.deckassemble.collections.domain.CollectionImportPreviewRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Parses, resolves, previews, and commits collection CSV imports. */
@Service
public class CollectionImportService {

    static final int MAX_FILE_BYTES = 1024 * 1024;
    static final int MAX_ROWS = 5000;
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private final CardReferenceResolver resolver;
    private final CollectionImportPreviewRepository previewRepository;
    private final CollectionService collectionService;
    private final CollectionAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public CollectionImportService(
            CardReferenceResolver resolver,
            CollectionImportPreviewRepository previewRepository,
            CollectionService collectionService,
            CollectionAccessGuard accessGuard,
            ObjectMapper objectMapper) {
        this.resolver = resolver;
        this.previewRepository = previewRepository;
        this.collectionService = collectionService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    public Preview preview(CollectionCsvParser.ColumnLayout layout, byte[] source) {
        List<CollectionCsvParser.ParsedRow> parsed = parse(layout, source);
        PreviewRows rows = PreviewRows.resolve(parsed, resolver);
        UUID token = UUID.randomUUID();
        previewRepository.save(
                new CollectionImportPreview(
                        token,
                        accessGuard.profileId(),
                        Instant.now().plus(PREVIEW_TTL),
                        sha256(source),
                        objectMapper.writeValueAsString(rows)));
        return new Preview(token, rows, Totals.from(rows));
    }

    @Transactional
    public CommitResult commit(
            UUID token, String name, Set<Integer> excludedLineNumbers, String idempotencyKey) {
        long profileId = accessGuard.lockedProfileId();
        CollectionImportPreview preview = ownedLockedPreview(token, profileId);
        if (preview.getStatus() != CollectionImportPreview.Status.PENDING) {
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

    @Transactional
    public byte[] errors(UUID token) {
        CollectionImportPreview preview = ownedPreview(token, accessGuard.profileId());
        PreviewRows rows =
                preview.getStatus() == CollectionImportPreview.Status.COMMITTED
                        ? snapshotFrom(preview).rows()
                        : rowsFrom(preview);
        return CollectionImportErrorExporter.export(rows);
    }

    private List<CollectionCsvParser.ParsedRow> parse(
            CollectionCsvParser.ColumnLayout layout, byte[] source) {
        if (source.length > MAX_FILE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Import file is too large");
        }
        var parsed = CollectionCsvParser.parse(new String(source, StandardCharsets.UTF_8), layout);
        if (parsed.size() > MAX_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Import has too many rows");
        }
        return parsed;
    }

    private CommitResult commitPreview(
            CollectionImportPreview preview,
            PreviewRows rows,
            String name,
            Set<Integer> excluded,
            String idempotencyKey) {
        CollectionResponse collection =
                collectionService.create(new CollectionCreateRequest(name, "", false));
        int imported = addSelected(collection.id(), rows.resolved(), excluded);
        int skipped = excludedCount(rows, excluded);
        preview.storeCanonicalRows(
                objectMapper.writeValueAsString(
                        new CommitSnapshot(rows, collection, imported, skipped)));
        preview.markCommitted(idempotencyKey, collection.id());
        previewRepository.save(preview);
        return new CommitResult(collection, imported, skipped);
    }

    private int addSelected(long collectionId, List<ResolvedRow> rows, Set<Integer> excluded) {
        var selected =
                rows.stream().filter(row -> !excluded.contains(row.row().lineNumber())).toList();
        selected.forEach(
                row ->
                        collectionService.addCard(
                                collectionId,
                                new CollectionCardAddRequest(
                                        row.printingId(), row.row().quantity(), 0)));
        return selected.size();
    }

    private CollectionImportPreview ownedLockedPreview(UUID token, long profileId) {
        return owned(
                previewRepository
                        .findLockedByTokenAndProfileId(token, profileId)
                        .orElseThrow(() -> notFound("Import preview not found")));
    }

    private CollectionImportPreview ownedPreview(UUID token, long profileId) {
        return owned(
                previewRepository
                        .findByTokenAndProfileId(token, profileId)
                        .orElseThrow(() -> notFound("Import preview not found")));
    }

    private CollectionImportPreview owned(CollectionImportPreview preview) {
        if (preview.getExpiresAt().isBefore(Instant.now())) {
            throw notFound("Import preview not found");
        }
        return preview;
    }

    private PreviewRows rowsFrom(CollectionImportPreview preview) {
        return objectMapper.readValue(preview.getCanonicalRows(), PreviewRows.class);
    }

    private CommitSnapshot snapshotFrom(CollectionImportPreview preview) {
        return objectMapper.readValue(preview.getCanonicalRows(), CommitSnapshot.class);
    }

    private void rejectUnresolved(PreviewRows rows, Set<Integer> excluded) {
        var unresolved =
                java.util.stream.Stream.of(
                                rows.ambiguous().stream().map(AmbiguousRow::row),
                                rows.unmatched().stream().map(UnmatchedRow::row),
                                rows.invalid().stream().map(InvalidRow::row))
                        .flatMap(java.util.function.Function.identity())
                        .filter(row -> !excluded.contains(row.lineNumber()))
                        .findAny();
        if (unresolved.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unresolved import rows must be excluded");
        }
    }

    private CommitResult resultFor(CollectionImportPreview preview) {
        if (preview.getCommittedCollectionId() == null) {
            throw new IllegalStateException("Committed import has no collection");
        }
        CommitSnapshot snapshot = snapshotFrom(preview);
        return new CommitResult(snapshot.collection(), snapshot.imported(), snapshot.skipped());
    }

    private CommitResult replayCommitted(CollectionImportPreview preview, String idempotencyKey) {
        if (!Objects.equals(preview.getIdempotencyKey(), idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Import preview is committed");
        }
        return resultFor(preview);
    }

    private static int excludedCount(PreviewRows rows, Set<Integer> excluded) {
        return (int)
                java.util.stream.Stream.of(
                                rows.resolved().stream().map(ResolvedRow::row),
                                rows.ambiguous().stream().map(AmbiguousRow::row),
                                rows.unmatched().stream().map(UnmatchedRow::row),
                                rows.invalid().stream().map(InvalidRow::row))
                        .flatMap(java.util.function.Function.identity())
                        .filter(row -> excluded.contains(row.lineNumber()))
                        .count();
    }

    private static String sha256(byte[] source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }

    public record Preview(UUID token, PreviewRows rows, Totals totals) {}

    public record PreviewRows(
            List<ResolvedRow> resolved,
            List<AmbiguousRow> ambiguous,
            List<UnmatchedRow> unmatched,
            List<InvalidRow> invalid) {

        static PreviewRows resolve(
                List<CollectionCsvParser.ParsedRow> parsedRows, CardReferenceResolver resolver) {
            var rows = new MutableRows();
            parsedRows.forEach(parsed -> rows.add(parsed, resolver));
            return rows.immutable();
        }
    }

    public record Row(int lineNumber, int quantity, CardReference reference) {}

    public record ResolvedRow(Row row, Long printingId) {

        ResolvedRow sum(ResolvedRow other) {
            int quantity = row.quantity() + other.row().quantity();
            return new ResolvedRow(
                    new Row(row.lineNumber(), quantity, row.reference()), printingId);
        }
    }

    public record AmbiguousRow(Row row, List<Long> printingIds) {}

    public record UnmatchedRow(Row row) {}

    public record InvalidRow(Row row, String error) {}

    public record Totals(int total, int resolved, int ambiguous, int unmatched, int invalid) {

        static Totals from(PreviewRows rows) {
            int resolved = rows.resolved().size();
            int ambiguous = rows.ambiguous().size();
            int unmatched = rows.unmatched().size();
            int invalid = rows.invalid().size();
            return new Totals(
                    resolved + ambiguous + unmatched + invalid,
                    resolved,
                    ambiguous,
                    unmatched,
                    invalid);
        }
    }

    public record CommitResult(CollectionResponse collection, int imported, int skipped) {}

    public record CommitSnapshot(
            PreviewRows rows, CollectionResponse collection, int imported, int skipped) {}

    private static final class MutableRows {
        private final List<ResolvedRow> resolved = new ArrayList<>();
        private final List<AmbiguousRow> ambiguous = new ArrayList<>();
        private final List<UnmatchedRow> unmatched = new ArrayList<>();
        private final List<InvalidRow> invalid = new ArrayList<>();

        private void add(CollectionCsvParser.ParsedRow parsed, CardReferenceResolver resolver) {
            Row row = new Row(parsed.lineNumber(), parsed.quantity(), parsed.reference());
            if (parsed.error() != null) {
                invalid.add(new InvalidRow(row, parsed.error()));
                return;
            }
            switch (resolver.resolve(parsed.reference())) {
                case CardReferenceResolution.Matched matched ->
                        resolved.add(new ResolvedRow(row, matched.printingId()));
                case CardReferenceResolution.Ambiguous ambiguousRow ->
                        ambiguous.add(new AmbiguousRow(row, ambiguousRow.printingIds()));
                case CardReferenceResolution.Unmatched ignored ->
                        unmatched.add(new UnmatchedRow(row));
            }
        }

        private PreviewRows immutable() {
            return new PreviewRows(
                    aggregated(),
                    List.copyOf(ambiguous),
                    List.copyOf(unmatched),
                    List.copyOf(invalid));
        }

        private List<ResolvedRow> aggregated() {
            var byPrinting = new LinkedHashMap<Long, ResolvedRow>();
            resolved.forEach(row -> byPrinting.merge(row.printingId(), row, ResolvedRow::sum));
            return List.copyOf(byPrinting.values());
        }
    }
}
