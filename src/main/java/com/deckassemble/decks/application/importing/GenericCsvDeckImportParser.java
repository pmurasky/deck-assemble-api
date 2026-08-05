package com.deckassemble.decks.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.shared.csv.CsvLineSplitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class GenericCsvDeckImportParser implements DeckImportParser {

    @Override
    public String format() {
        return "GENERIC_CSV";
    }

    @Override
    public ParsedDeck parse(String source) {
        return parseCsv(source, format(), new CsvLayout("quantity", "set", "section"));
    }

    static ParsedDeck parseCsv(String source, String format, CsvLayout layout) {
        List<String> lines = source.lines().filter(line -> !line.isBlank()).toList();
        if (lines.isEmpty()) {
            return new ParsedDeck(Map.of("format", format), List.of());
        }
        Map<String, Integer> headers = headers(CsvLineSplitter.split(lines.getFirst()));
        List<ParsedRow> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            rows.add(row(index + 1, CsvLineSplitter.split(lines.get(index)), headers, layout));
        }
        return new ParsedDeck(Map.of("format", format), List.copyOf(rows));
    }

    private static ParsedRow row(
            int lineNumber, List<String> values, Map<String, Integer> headers, CsvLayout layout) {
        try {
            int quantity = Integer.parseInt(value(values, headers, layout.quantityHeader()));
            String name = value(values, headers, "name");
            String set = value(values, headers, layout.setHeader());
            String collector = value(values, headers, "collector number", "collector_number");
            return new ParsedRow(
                    lineNumber,
                    quantity,
                    section(value(values, headers, layout.sectionHeader())),
                    new CardReference(scryfallId(values, headers), name, set, collector),
                    null);
        } catch (IllegalArgumentException exception) {
            var reference = new CardReference(null, String.join(",", values), null, null);
            return new ParsedRow(
                    lineNumber, 0, DeckCard.Section.MAIN_DECK, reference, exception.getMessage());
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

    private static @Nullable UUID scryfallId(List<String> values, Map<String, Integer> headers) {
        for (String name : List.of("scryfall id", "scryfall_id")) {
            Integer index = headers.get(name);
            if (index != null && index < values.size() && !values.get(index).isBlank()) {
                return UUID.fromString(values.get(index).strip());
            }
        }
        return null;
    }

    private static String value(
            List<String> values, Map<String, Integer> headers, String... names) {
        for (String name : names) {
            Integer index = headers.get(name.toLowerCase(Locale.ROOT));
            if (index != null && index < values.size() && !values.get(index).isBlank()) {
                return values.get(index).strip();
            }
        }
        throw new IllegalArgumentException("Missing required CSV value");
    }

    private static DeckCard.Section section(String value) {
        String normalized = value.replace(" ", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "commander", "commanders" -> DeckCard.Section.COMMANDER;
            case "sideboard" -> DeckCard.Section.SIDEBOARD;
            case "companion", "companions" -> DeckCard.Section.COMPANION;
            case "maybeboard" -> DeckCard.Section.MAYBE_BOARD;
            default -> DeckCard.Section.MAIN_DECK;
        };
    }

    record CsvLayout(String quantityHeader, String setHeader, String sectionHeader) {}
}
