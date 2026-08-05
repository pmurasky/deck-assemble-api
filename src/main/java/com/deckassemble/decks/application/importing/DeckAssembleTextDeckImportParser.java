package com.deckassemble.decks.application.importing;

import com.deckassemble.cards.application.CardReference;
import com.deckassemble.decks.domain.DeckCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DeckAssembleTextDeckImportParser implements DeckImportParser {

    private static final Pattern ROW =
            Pattern.compile(
                    "^(?<quantity>\\d+)\\s+(?<name>.+)\\|(?<set>[^|]+)\\|(?<collector>[^|]+)$");
    private static final Map<String, DeckCard.Section> HEADINGS =
            Map.ofEntries(
                    Map.entry("commander", DeckCard.Section.COMMANDER),
                    Map.entry("commanders", DeckCard.Section.COMMANDER),
                    Map.entry("deck", DeckCard.Section.MAIN_DECK),
                    Map.entry("main", DeckCard.Section.MAIN_DECK),
                    Map.entry("main deck", DeckCard.Section.MAIN_DECK),
                    Map.entry("mainboard", DeckCard.Section.MAIN_DECK),
                    Map.entry("sideboard", DeckCard.Section.SIDEBOARD),
                    Map.entry("companion", DeckCard.Section.COMPANION),
                    Map.entry("companions", DeckCard.Section.COMPANION),
                    Map.entry("maybeboard", DeckCard.Section.MAYBE_BOARD),
                    Map.entry("maybe board", DeckCard.Section.MAYBE_BOARD));

    @Override
    public String format() {
        return "DECKASSEMBLE_TEXT";
    }

    @Override
    public ParsedDeck parse(String source) {
        return parseText(source, format(), ROW);
    }

    static ParsedDeck parseText(String source, String format, Pattern rowPattern) {
        List<ParsedRow> rows = new ArrayList<>();
        DeckCard.Section section = DeckCard.Section.MAIN_DECK;
        String[] lines = source.lines().toArray(String[]::new);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty()) {
                continue;
            }
            DeckCard.Section heading = heading(line);
            if (heading != null) {
                section = heading;
            } else {
                rows.add(row(index + 1, section, line, rowPattern));
            }
        }
        return new ParsedDeck(Map.of("format", format), List.copyOf(rows));
    }

    private static ParsedRow row(
            int lineNumber, DeckCard.Section section, String line, Pattern rowPattern) {
        var matcher = rowPattern.matcher(line);
        if (!matcher.matches()) {
            return invalidRow(lineNumber, section, line, "Invalid row");
        }
        try {
            return validRow(lineNumber, section, matcher);
        } catch (NumberFormatException exception) {
            return invalidRow(lineNumber, section, line, "Invalid quantity");
        }
    }

    private static ParsedRow validRow(int lineNumber, DeckCard.Section section, Matcher matcher) {
        var reference =
                new CardReference(
                        null,
                        matcher.group("name"),
                        matcher.group("set"),
                        matcher.group("collector"));
        return new ParsedRow(
                lineNumber, Integer.parseInt(matcher.group("quantity")), section, reference, null);
    }

    private static ParsedRow invalidRow(
            int lineNumber, DeckCard.Section section, String line, String error) {
        return new ParsedRow(
                lineNumber, 0, section, new CardReference(null, line, null, null), error);
    }

    private static DeckCard.@Nullable Section heading(String line) {
        String value = line.replace("[", "").replace("]", "").toLowerCase(Locale.ROOT);
        return HEADINGS.get(value);
    }
}
