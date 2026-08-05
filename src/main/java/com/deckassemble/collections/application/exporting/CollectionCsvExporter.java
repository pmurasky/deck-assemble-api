package com.deckassemble.collections.application.exporting;

import com.deckassemble.cards.application.CardExportView;
import com.deckassemble.collections.application.CollectionCardResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Renders collection cards as a deterministic CSV download. */
public final class CollectionCsvExporter {

    private static final String HEADER =
            "scryfall_id,name,set,collector_number,quantity,printing_id\n";
    private static final Comparator<Row> ROW_ORDER =
            Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Row::setCode)
                    .thenComparing(Row::collectorNumber)
                    .thenComparing(Row::scryfallId);

    private CollectionCsvExporter() {}

    public static byte[] export(
            List<CollectionCardResponse> cards, Map<Long, CardExportView> views) {
        var csv = new StringBuilder(HEADER);
        cards.stream()
                .map(card -> row(card, views))
                .sorted(ROW_ORDER)
                .forEach(row -> append(csv, row));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static Row row(CollectionCardResponse card, Map<Long, CardExportView> views) {
        CardExportView view = views.get(card.cardPrintingId());
        if (view == null) {
            throw new IllegalStateException("Collection references a missing card printing");
        }
        var printing = view.printing();
        int quantity = card.regularQuantity() + card.foilQuantity();
        return new Row(
                printing.scryfallId(),
                view.displayName(),
                printing.setCode(),
                printing.collectorNumber(),
                quantity,
                view.printingId());
    }

    private static void append(StringBuilder csv, Row row) {
        csv.append(escape(row.scryfallId()))
                .append(',')
                .append(escape(row.name()))
                .append(',')
                .append(escape(row.setCode()))
                .append(',')
                .append(escape(row.collectorNumber()))
                .append(',')
                .append(row.quantity())
                .append(',')
                .append(row.printingId())
                .append('\n');
    }

    private static String escape(@Nullable String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private record Row(
            String scryfallId,
            String name,
            String setCode,
            String collectorNumber,
            int quantity,
            long printingId) {}
}
