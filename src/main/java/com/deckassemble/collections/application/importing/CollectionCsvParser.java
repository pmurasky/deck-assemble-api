package com.deckassemble.collections.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.shared.csv.CsvLineSplitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/** Parses collection CSV sources into rows using an explicit column layout. */
public final class CollectionCsvParser {

    private CollectionCsvParser() {}

    public static List<ParsedRow> parse(String source, ColumnLayout layout) {
        List<String> lines = source.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> headers = headers(CsvLineSplitter.split(lines.getFirst()));
        List<ParsedRow> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            rows.add(row(index + 1, CsvLineSplitter.split(lines.get(index)), headers, layout));
        }
        return List.copyOf(rows);
    }

    private static ParsedRow row(
            int lineNumber,
            List<String> values,
            Map<String, Integer> headers,
            ColumnLayout layout) {
        String name = value(values, headers, layout.nameHeader());
        String setCode = value(values, headers, layout.setCodeHeader());
        String collectorNumber = value(values, headers, layout.collectorNumberHeader());
        try {
            int quantity = quantity(value(values, headers, layout.quantityHeader()));
            var reference =
                    new CardReference(
                            scryfallId(values, headers, layout.scryfallIdHeader()),
                            requireName(name),
                            setCode,
                            collectorNumber);
            return new ParsedRow(lineNumber, quantity, reference, null);
        } catch (IllegalArgumentException exception) {
            var fallback = new CardReference(null, name, setCode, collectorNumber);
            return new ParsedRow(lineNumber, 0, fallback, exception.getMessage());
        }
    }

    private static String requireName(@Nullable String name) {
        if (name == null) {
            throw new IllegalArgumentException("Missing card name");
        }
        return name;
    }

    private static int quantity(@Nullable String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Missing quantity");
        }
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity < 1) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid quantity '" + raw + "'", exception);
        }
    }

    private static Map<String, Integer> headers(List<String> values) {
        return IntStream.range(0, values.size())
                .boxed()
                .collect(
                        Collectors.toUnmodifiableMap(
                                index -> values.get(index).strip().toLowerCase(Locale.ROOT),
                                index -> index));
    }

    private static @Nullable UUID scryfallId(
            List<String> values, Map<String, Integer> headers, @Nullable String header) {
        String raw = value(values, headers, header);
        return raw == null ? null : UUID.fromString(raw);
    }

    private static @Nullable String value(
            List<String> values, Map<String, Integer> headers, @Nullable String header) {
        if (header == null) {
            return null;
        }
        Integer index = headers.get(header.toLowerCase(Locale.ROOT));
        if (index == null || index >= values.size() || values.get(index).isBlank()) {
            return null;
        }
        return values.get(index).strip();
    }

    public record ColumnLayout(
            String quantityHeader,
            String nameHeader,
            @Nullable String setCodeHeader,
            @Nullable String collectorNumberHeader,
            @Nullable String scryfallIdHeader) {}

    public record ParsedRow(
            int lineNumber, int quantity, CardReference reference, @Nullable String error) {}
}
