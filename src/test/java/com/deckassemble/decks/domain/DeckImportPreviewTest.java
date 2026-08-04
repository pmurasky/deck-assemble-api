package com.deckassemble.decks.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeckImportPreviewTest {

    @Test
    void shouldRecordCommittedDeck() {
        var preview = new DeckImportPreview(UUID.randomUUID(), 1L, Instant.MAX, "sha256", "[]");

        preview.markCommitted("request-key", 2L);

        assertThat(preview.getIdempotencyKey()).isEqualTo("request-key");
        assertThat(preview.getCommittedDeckId()).isEqualTo(2L);
        assertThat(preview.getStatus()).isEqualTo(DeckImportPreview.Status.COMMITTED);
    }
}
