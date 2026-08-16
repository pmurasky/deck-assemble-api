package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BeginnerGuideRepositoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired private CardRepository cardRepository;
    @Autowired private BeginnerGuideRepository guideRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldPersistDraftContent() {
        var card = cardRepository.saveAndFlush(new Card("oracle-id", "Test Card"));
        var generatedAt = OffsetDateTime.parse("2026-08-16T12:00:00Z");
        var draft =
                new BeginnerGuideDraft(
                        "Summary", "Examples", "When to use", "Ruling text", "a".repeat(64));
        guideRepository.saveAndFlush(new BeginnerGuide(card.getId(), draft, generatedAt));
        entityManager.clear();

        var reloaded = guideRepository.findById(card.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BeginnerGuideStatus.DRAFT);
        assertThat(reloaded.getSummary()).isEqualTo("Summary");
        assertThat(reloaded.getSourceRulingsSnapshot()).isEqualTo("Ruling text");
        assertThat(reloaded.getSourceOracleHash()).hasSize(64);
        assertThat(reloaded.getGeneratedAt()).isEqualTo(generatedAt);
    }
}
