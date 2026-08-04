package com.deckassemble.decks.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.cards.application.CardReferenceResolution;
import com.deckassemble.cards.application.CardReferenceResolver;
import com.deckassemble.decks.application.DeckAccessGuard;
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
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Parses, resolves, and persists deck import previews without mutating decks. */
@Service
public class DeckImportService {

    static final int MAX_FILE_BYTES = 1024 * 1024;
    static final int MAX_ROWS = 500;
    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private final Map<String, DeckImportParser> parsers;
    private final CardReferenceResolver resolver;
    private final DeckImportPreviewRepository previewRepository;
    private final DeckAccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public DeckImportService(
            List<DeckImportParser> parsers,
            CardReferenceResolver resolver,
            DeckImportPreviewRepository previewRepository,
            DeckAccessGuard accessGuard,
            ObjectMapper objectMapper) {
        this.parsers =
                parsers.stream()
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        DeckImportParser::format, parser -> parser));
        this.resolver = resolver;
        this.previewRepository = previewRepository;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    public Preview preview(String format, byte[] source) {
        DeckImportParser.ParsedDeck parsed = parse(format, source);
        PreviewRows rows = PreviewRows.resolve(parsed.rows(), resolver);
        UUID token = UUID.randomUUID();
        previewRepository.save(
                new DeckImportPreview(
                        token,
                        accessGuard.profileId(),
                        Instant.now().plus(PREVIEW_TTL),
                        sha256(source),
                        objectMapper.writeValueAsString(rows)));
        return new Preview(token, parsed.metadata(), rows, Totals.from(rows));
    }

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
