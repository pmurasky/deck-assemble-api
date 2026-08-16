package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeginnerGuideGenerationQuotaTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-16T12:00:00Z"), ZoneOffset.UTC);

    @Mock private CurrentUser currentUser;
    @Mock private BeginnerGuideRepository guideRepository;

    @Test
    void shouldRejectRequesterAtDailyLimit() {
        when(currentUser.subject()).thenReturn(Optional.of("user-1"));
        when(guideRepository.countGeneratedByBetween(
                        "user-1",
                        OffsetDateTime.parse("2026-08-16T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-17T00:00:00Z")))
                .thenReturn(5L);
        var quota = new BeginnerGuideGenerationQuota(currentUser, guideRepository, CLOCK, 5);

        assertThatThrownBy(quota::requireAvailable)
                .isInstanceOf(BeginnerGuideDailyLimitExceededException.class);

        verify(guideRepository)
                .countGeneratedByBetween(
                        "user-1",
                        OffsetDateTime.parse("2026-08-16T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-17T00:00:00Z"));
    }
}
