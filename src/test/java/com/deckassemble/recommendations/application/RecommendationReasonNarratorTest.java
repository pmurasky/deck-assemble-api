package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecommendationReasonNarratorTest {

    @Test
    void shouldRenderNonEmptySentenceForEveryReasonCode() {
        // Given / When / Then
        for (RecommendationReasonCode code : RecommendationReasonCode.values()) {
            assertThat(RecommendationReasonNarrator.render(code, Map.of()))
                    .as("reason for %s", code)
                    .isNotBlank();
        }
    }

    @Test
    void shouldSubstituteEvidenceValuesIntoTemplate() {
        // Given
        var evidence = Map.of("playStyle", "tokens");

        // When
        var reason =
                RecommendationReasonNarrator.render(RecommendationReasonCode.PLAY_STYLE, evidence);

        // Then
        assertThat(reason).contains("tokens").doesNotContain("{playStyle}");
    }

    @Test
    void shouldKeepSentenceReadableWhenEvidenceIsMissing() {
        // Given / When
        var reason = RecommendationReasonNarrator.render(RecommendationReasonCode.BUDGET, Map.of());

        // Then
        assertThat(reason).isNotBlank().doesNotContain("{").doesNotContain("}");
    }

    @Test
    void shouldRenderOwnedReason() {
        // Given / When
        var reason = RecommendationReasonNarrator.render(RecommendationReasonCode.OWNED, Map.of());

        // Then
        assertThat(reason).containsIgnoringCase("own");
    }

    @Test
    void shouldExposeReasonAlongsideScoreContribution() {
        // Given
        var contribution =
                new ScoreContribution(
                        RecommendationReasonCode.PLAY_STYLE,
                        BigDecimal.ZERO,
                        Map.of("playStyle", "go-wide"));

        // When / Then
        assertThat(contribution.reason()).isEqualTo("Matches your go-wide play style.");
    }
}
