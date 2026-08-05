package com.deckassemble.shared.csv;

import java.util.ArrayList;
import java.util.List;

/** Splits one CSV record into raw values, honoring double-quoted sections. */
public final class CsvLineSplitter {

    private CsvLineSplitter() {}

    public static List<String> split(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(character);
            }
        }
        values.add(value.toString());
        return values;
    }
}
