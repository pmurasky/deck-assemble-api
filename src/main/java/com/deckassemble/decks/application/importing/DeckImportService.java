package com.deckassemble.decks.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.cards.application.CardReferenceResolution;
import com.deckassemble.cards.application.CardReferenceResolver;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckImportPreview;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Previews and atomically commits external deck imports. */
@Service
public class DeckImportService {

    static final int MAX_FILE_BYTES = 1024 * 1024;
    static final int MAX_ROWS = 500;
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private final Map<String, DeckImportParser> parsers;
    private final CardReferenceResolver resolver;
    private final Dependencies dependencies;

    public DeckImportService(
            List<DeckImportParser> parsers,
            CardReferenceResolver resolver,
            Dependencies dependencies) {
        this.parsers =
                parsers.stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        DeckImportParser::format, parser -> parser));
        this.resolver = resolver;
        this.dependencies = dependencies;
    }

    public Preview preview(String format, byte[] source) {
        DeckImportParser.ParsedDeck parsed = parse(format, source);
        PreviewRows rows = PreviewRows.resolve(parsed.rows(), resolver);
        UUID token = UUID.randomUUID();
        dependencies
                .previewRepository()
                .save(
                        new DeckImportPreview(
                                token,
                                dependencies.accessGuard().profileId(),
                                Instant.now().plus(PREVIEW_TTL),
                                sha256(source),
                                dependencies.objectMapper().writeValueAsString(rows)));
        return new Preview(token, parsed.metadata(), rows, Totals.from(rows));
    }

    @Transactional
    public CommitResult commit(
            UUID token, String name, Set<Integer> excludedLineNumbers, String idempotencyKey) {
        long profileId = dependencies.accessGuard().profileId();
        var replay =
                dependencies
                        .previewRepository()
                        .findByProfileIdAndIdempotencyKey(profileId, idempotencyKey);
        if (replay.isPresent()) {
            return resultFor(replay.orElseThrow(), excludedLineNumbers);
        }
        DeckImportPreview preview = ownedPreview(token, profileId);
        PreviewRows rows = rowsFrom(preview);
        if (preview.getStatus() != DeckImportPreview.Status.PENDING) {
            return replayCommitted(preview, excludedLineNumbers, idempotencyKey);
        }
        rejectUnresolved(rows, excludedLineNumbers);
        return commitPreview(preview, rows, name, excludedLineNumbers, idempotencyKey);
    }

    private CommitResult commitPreview(
            DeckImportPreview preview,
            PreviewRows rows,
            String name,
            Set<Integer> excluded,
            String idempotencyKey) {
        DeckResponse deck = dependencies.deckService().create(deckRequest(name));
        int imported = addSelected(deck.id(), rows.resolved(), excluded);
        preview.markCommitted(idempotencyKey, deck.id());
        dependencies.previewRepository().save(preview);
        return new CommitResult(deck, imported, excludedCount(rows, excluded));
    }

    private DeckImportPreview ownedPreview(UUID token, long profileId) {
        DeckImportPreview preview =
                dependencies
                        .previewRepository()
                        .findLockedByTokenAndProfileId(token, profileId)
                        .orElseThrow(() -> notFound("Import preview not found"));
        if (preview.getExpiresAt().isBefore(Instant.now())) {
            throw notFound("Import preview not found");
        }
        return preview;
    }

    private PreviewRows rowsFrom(DeckImportPreview preview) {
        return dependencies.objectMapper().readValue(preview.getCanonicalRows(), PreviewRows.class);
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

    private int addSelected(long deckId, List<ResolvedRow> rows, Set<Integer> excluded) {
        var selected =
                rows.stream().filter(row -> !excluded.contains(row.row().lineNumber())).toList();
        selected.forEach(
                row ->
                        dependencies
                                .deckCardService()
                                .addCard(
                                        deckId,
                                        new DeckCardAddRequest(
                                                row.printingId(),
                                                row.row().quantity(),
                                                row.row().section())));
        return selected.size();
    }

    private CommitResult resultFor(DeckImportPreview preview, Set<Integer> excluded) {
        Long deckId = preview.getCommittedDeckId();
        if (deckId == null) {
            throw new IllegalStateException("Committed import has no deck");
        }
        PreviewRows rows = rowsFrom(preview);
        int imported =
                (int)
                        rows.resolved().stream()
                                .filter(row -> !excluded.contains(row.row().lineNumber()))
                                .count();
        return new CommitResult(
                dependencies.deckService().getById(deckId),
                imported,
                excludedCount(rows, excluded));
    }

    private CommitResult replayCommitted(
            DeckImportPreview preview, Set<Integer> excluded, String idempotencyKey) {
        if (!Objects.equals(preview.getIdempotencyKey(), idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Import preview is committed");
        }
        return resultFor(preview, excluded);
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

    private static DeckCreateRequest deckRequest(String name) {
        return new DeckCreateRequest(name, "COMMANDER", null, null, null, false, null, null, null);
    }

    private static ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }

    @Component
    public record Dependencies(
            DeckImportPreviewRepository previewRepository,
            DeckAccessGuard accessGuard,
            ObjectMapper objectMapper,
            DeckService deckService,
            DeckCardService deckCardService) {}

    private DeckImportParser.ParsedDeck parse(String format, byte[] source) {
        if (source.length > MAX_FILE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Import file is too large");
        }
        DeckImportParser parser = parsers.get(format);
        if (parser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported import format");
        }
        var parsed = parser.parse(new String(source, StandardCharsets.UTF_8));
        if (parsed.rows().size() > MAX_ROWS) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Import has too many rows");
        }
        return parsed;
    }

    private static String sha256(byte[] source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Preview(
            UUID token, Map<String, String> metadata, PreviewRows rows, Totals totals) {}

    public record CommitResult(DeckResponse deck, int imported, int skipped) {}

    public record PreviewRows(
            List<ResolvedRow> resolved,
            List<AmbiguousRow> ambiguous,
            List<UnmatchedRow> unmatched,
            List<InvalidRow> invalid) {

        static PreviewRows resolve(
                List<DeckImportParser.ParsedRow> parsedRows, CardReferenceResolver resolver) {
            var rows = new MutableRows();
            for (var parsed : parsedRows) {
                Row row = Row.from(parsed);
                if (parsed.error() != null) {
                    rows.invalid.add(new InvalidRow(row, parsed.error()));
                    continue;
                }
                switch (resolver.resolve(parsed.reference())) {
                    case CardReferenceResolution.Matched matched ->
                            rows.resolved.add(
                                    new ResolvedRow(row, matched.cardId(), matched.printingId()));
                    case CardReferenceResolution.Ambiguous ambiguous ->
                            rows.ambiguous.add(new AmbiguousRow(row, ambiguous.printingIds()));
                    case CardReferenceResolution.Unmatched ignored ->
                            rows.unmatched.add(new UnmatchedRow(row));
                }
            }
            return rows.immutable();
        }
    }

    public record Row(
            int lineNumber, int quantity, DeckCard.Section section, CardReference reference) {

        static Row from(DeckImportParser.ParsedRow row) {
            return new Row(row.lineNumber(), row.quantity(), row.section(), row.reference());
        }
    }

    public record ResolvedRow(Row row, Long cardId, Long printingId) {}

    public record AmbiguousRow(Row row, List<Long> printingIds) {}

    public record UnmatchedRow(Row row) {}

    public record InvalidRow(Row row, String error) {}

    public record Totals(int total, int resolved, int ambiguous, int unmatched, int invalid) {

        static Totals from(PreviewRows rows) {
            int total =
                    rows.resolved().size()
                            + rows.ambiguous().size()
                            + rows.unmatched().size()
                            + rows.invalid().size();
            return new Totals(
                    total,
                    rows.resolved().size(),
                    rows.ambiguous().size(),
                    rows.unmatched().size(),
                    rows.invalid().size());
        }
    }

    private static final class MutableRows {
        private final List<ResolvedRow> resolved = new ArrayList<>();
        private final List<AmbiguousRow> ambiguous = new ArrayList<>();
        private final List<UnmatchedRow> unmatched = new ArrayList<>();
        private final List<InvalidRow> invalid = new ArrayList<>();

        private PreviewRows immutable() {
            return new PreviewRows(
                    List.copyOf(resolved),
                    List.copyOf(ambiguous),
                    List.copyOf(unmatched),
                    List.copyOf(invalid));
        }
    }
}
