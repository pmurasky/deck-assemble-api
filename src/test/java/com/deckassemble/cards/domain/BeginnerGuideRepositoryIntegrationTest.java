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

    @Test
    void shouldCountUserGenerationsWithinDay() {
        var firstCard = cardRepository.saveAndFlush(new Card("oracle-one", "First Card"));
        var secondCard = cardRepository.saveAndFlush(new Card("oracle-two", "Second Card"));
        var dayStart = OffsetDateTime.parse("2026-08-16T00:00:00Z");
        var draft =
                new BeginnerGuideDraft(
                        "Summary", "Examples", "When to use", "Ruling", "a".repeat(64));
        guideRepository.saveAndFlush(
                new BeginnerGuide(firstCard.getId(), draft, dayStart.plusHours(1), "user-1"));
        guideRepository.saveAndFlush(
                new BeginnerGuide(secondCard.getId(), draft, dayStart.plusDays(1), "user-1"));

        var count =
                guideRepository.countGeneratedByBetween("user-1", dayStart, dayStart.plusDays(1));

        assertThat(count).isEqualTo(1);
    }
}
