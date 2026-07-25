package com.deckassemble.recommendations.infrastructure.spellbook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RestClientCommanderSpellbookClientTest {

    @Test
    void shouldParseOnlyFullyIncludedCombos() {
        var combos = client().parse(payload());

        assertThat(combos).hasSize(1);
        assertThat(combos.get(0).id()).isEqualTo("combo-1");
        assertThat(combos.get(0).cards()).containsExactly("Sol Ring", "Hullbreaker Horror");
        assertThat(combos.get(0).produces()).containsExactly("Infinite colorless mana");
        assertThat(combos.get(0).prerequisites()).isEqualTo("Control a permanent.");
    }

    private RestClientCommanderSpellbookClient client() {
        return new RestClientCommanderSpellbookClient(
                new CommanderSpellbookProperties(
                        "https://example.com",
                        "test",
                        java.time.Duration.ofSeconds(1),
                        java.time.Duration.ofSeconds(1)),
                JsonMapper.builder().build());
    }

    private static String payload() {
        return """
                {"results":{"included":[{"id":"combo-1","uses":[{"card":{"name":"Sol Ring"}},{"card":{"name":"Hullbreaker Horror"}}],"produces":[{"feature":{"name":"Infinite colorless mana"}}],"description":"Loop.","notablePrerequisites":"Control a permanent."}],"almostIncluded":[{"id":"not-complete"}]}}
                """;
    }
}
