package com.deckassemble.collections.application.importing;

import com.deckassemble.collections.application.importing.CollectionImportService.PreviewRows;
import com.deckassemble.collections.application.importing.CollectionImportService.Row;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Renders rejected collection import rows as a CSV download with reason codes. */
public final class CollectionImportErrorExporter {

    private static final String HEADER =
            "line_number,reason,quantity,name,set_code,collector_number,scryfall_id,detail\n";

    private CollectionImportErrorExporter() {}

    public static byte[] export(PreviewRows rows) {
        var csv = new StringBuilder(HEADER);
        rows.ambiguous()
                .forEach(
                        row -> append(csv, row.row(), "AMBIGUOUS", identifiers(row.printingIds())));
        rows.unmatched().forEach(row -> append(csv, row.row(), "UNMATCHED", ""));
        rows.invalid().forEach(row -> append(csv, row.row(), "INVALID", row.error()));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void append(StringBuilder csv, Row row, String reason, @Nullable String detail) {
        var reference = row.reference();
        csv.append(row.lineNumber())
                .append(',')
                .append(reason)
                .append(',')
                .append(row.quantity())
                .append(',')
                .append(escape(reference.name()))
                .append(',')
                .append(escape(reference.setCode()))
                .append(',')
                .append(escape(reference.collectorNumber()))
                .append(',')
                .append(reference.scryfallId() == null ? "" : reference.scryfallId().toString())
                .append(',')
                .append(escape(detail))
                .append('\n');
    }

    private static String identifiers(List<Long> printingIds) {
        return printingIds.stream().map(String::valueOf).collect(Collectors.joining(";"));
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
}
