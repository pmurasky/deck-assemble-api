package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OracleTagIndexTest {

    @Test
    void shouldGroupTagLabelsByOracleId() {
        // Given two tags sharing one tagged card
        String jsonl =
                """
                {"object":"tag","label":"ramp","slug":"ramp","type":"oracle","taggings":[{"oracle_id":"card-1","weight":"median"},{"oracle_id":"card-2","weight":"median"}]}
                {"object":"tag","label":"landfall","slug":"landfall","type":"oracle","taggings":[{"oracle_id":"card-1","weight":"median"}]}
                """;

        // When parsing the tag file
        Map<String, Set<String>> index = OracleTagIndex.parse(stream(jsonl));

        // Then labels are inverted into a per-card index
        assertThat(index)
                .containsEntry("card-1", Set.of("ramp", "landfall"))
                .containsEntry("card-2", Set.of("ramp"));
    }

    @Test
    void shouldSkipLinesWithoutTaggings() {
        // Given a tag with no tagged cards
        String jsonl =
                "{\"object\":\"tag\",\"label\":\"unused\",\"type\":\"oracle\",\"taggings\":[]}\n";

        // When parsing
        Map<String, Set<String>> index = OracleTagIndex.parse(stream(jsonl));

        // Then no entries are produced
        assertThat(index).isEmpty();
    }

    @Test
    void shouldParseAnEmptyStream() {
        assertThat(OracleTagIndex.parse(stream(""))).isEmpty();
    }

    private ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
