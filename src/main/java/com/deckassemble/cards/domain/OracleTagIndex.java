package com.deckassemble.cards.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Inverts a Tagger tag JSONL stream (one line per tag, each listing tagged oracle ids) into a
 * per-card index of tag labels.
 */
public final class OracleTagIndex {

    private static final JsonMapper MAPPER = JsonMapper.shared();

    private OracleTagIndex() {}

    public static Map<String, Set<String>> parse(InputStream jsonl) {
        Map<String, Set<String>> index = new HashMap<>();
        try (var reader =
                new BufferedReader(new InputStreamReader(jsonl, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    indexTag(MAPPER.readTree(line), index);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to parse oracle tag stream", exception);
        }
        return index;
    }

    private static void indexTag(JsonNode tag, Map<String, Set<String>> index) {
        String label = tag.path("label").asString(null);
        JsonNode taggings = tag.path("taggings");
        if (label == null || !taggings.isArray()) {
            return;
        }
        for (JsonNode tagging : taggings) {
            String oracleId = tagging.path("oracle_id").asString(null);
            if (oracleId != null) {
                index.computeIfAbsent(oracleId, key -> new TreeSet<>()).add(label);
            }
        }
    }
}
