package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class BeginnerGuideTest {
    private static final OffsetDateTime GENERATED_AT = OffsetDateTime.parse("2026-08-16T12:00:00Z");
    private static final OffsetDateTime PUBLISHED_AT = OffsetDateTime.parse("2026-08-16T13:00:00Z");

    @Test
    void shouldMoveThroughReviewStatuses() {
        var guide = new BeginnerGuide(42L, draft(), GENERATED_AT);

        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.DRAFT);
        guide.publish("admin-subject", PUBLISHED_AT);
        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.PUBLISHED);
        assertThat(guide.getReviewedBy()).isEqualTo("admin-subject");
        assertThat(guide.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        guide.markStale();
        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.STALE);
        guide.report();
        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.REPORTED);
    }

    private BeginnerGuideDraft draft() {
        return new BeginnerGuideDraft(
                "Summary", "Examples", "When to use", "Ruling text", "a".repeat(64));
    }
}
