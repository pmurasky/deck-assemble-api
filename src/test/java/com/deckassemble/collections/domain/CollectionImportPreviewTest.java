package com.deckassemble.collections.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollectionImportPreviewTest {

    @Test
    void shouldRecordCommittedCollection() {
        var preview =
                new CollectionImportPreview(UUID.randomUUID(), 1L, Instant.MAX, "sha256", "[]");

        preview.markCommitted("request-key", 2L);

        assertThat(preview.getIdempotencyKey()).isEqualTo("request-key");
        assertThat(preview.getCommittedCollectionId()).isEqualTo(2L);
        assertThat(preview.getStatus()).isEqualTo(CollectionImportPreview.Status.COMMITTED);
    }
}
