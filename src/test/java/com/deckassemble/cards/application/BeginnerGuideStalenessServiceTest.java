package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideDraft;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideSource;
import com.deckassemble.cards.domain.BeginnerGuideStatus;
import com.deckassemble.cards.domain.Card;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BeginnerGuideStalenessServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-16T12:00:00Z");

    @Mock private BeginnerGuideRepository guideRepository;
    @InjectMocks private BeginnerGuideStalenessService service;

    @Test
    void shouldMarkPublishedGuideStaleWhenOracleTextChanges() {
        var card = new Card("oracle-id", "Card name");
        ReflectionTestUtils.setField(card, "id", 42L);
        card.setOracleText("New oracle text");
        var guide = new BeginnerGuide(42L, draft("a".repeat(64)), NOW);
        guide.publish("reviewer", NOW);
        when(guideRepository.findById(42L)).thenReturn(Optional.of(guide));

        service.markStaleIfOracleChanged(card);

        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.STALE);
        verify(guideRepository).save(guide);
    }

    @Test
    void shouldKeepPublishedGuideWhenOracleTextIsUnchanged() {
        var card = new Card("oracle-id", "Card name");
        ReflectionTestUtils.setField(card, "id", 42L);
        card.setOracleText("Unchanged oracle text");
        var hash = BeginnerGuideSource.fromCard(card, List.of()).oracleHash();
        var guide = new BeginnerGuide(42L, draft(hash), NOW);
        guide.publish("reviewer", NOW);
        when(guideRepository.findById(42L)).thenReturn(Optional.of(guide));

        service.markStaleIfOracleChanged(card);

        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.PUBLISHED);
        verify(guideRepository, never()).save(guide);
    }

    @Test
    void shouldKeepDraftGuideWhenOracleTextChanges() {
        var card = new Card("oracle-id", "Card name");
        ReflectionTestUtils.setField(card, "id", 42L);
        card.setOracleText("New oracle text");
        var guide = new BeginnerGuide(42L, draft("a".repeat(64)), NOW);
        when(guideRepository.findById(42L)).thenReturn(Optional.of(guide));

        service.markStaleIfOracleChanged(card);

        assertThat(guide.getStatus()).isEqualTo(BeginnerGuideStatus.DRAFT);
        verify(guideRepository, never()).save(guide);
    }

    private static BeginnerGuideDraft draft(String oracleHash) {
        return new BeginnerGuideDraft("Summary", "Examples", "When", "Rulings", oracleHash);
    }
}
