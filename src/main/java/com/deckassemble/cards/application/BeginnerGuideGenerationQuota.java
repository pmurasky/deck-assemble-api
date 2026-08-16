package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.shared.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Enforces the persisted per-user UTC daily generation limit. */
public final class BeginnerGuideGenerationQuota {
    private final CurrentUser currentUser;
    private final BeginnerGuideRepository guideRepository;
    private final Clock clock;
    private final int dailyLimit;

    public BeginnerGuideGenerationQuota(
            CurrentUser currentUser,
            BeginnerGuideRepository guideRepository,
            Clock clock,
            int dailyLimit) {
        this.currentUser = currentUser;
        this.guideRepository = guideRepository;
        this.clock = clock;
        this.dailyLimit = dailyLimit;
    }

    public String requireAvailable() {
        var requester = currentUser.subject().orElseThrow(IllegalStateException::new);
        var from = dayStart();
        var generated = guideRepository.countGeneratedByBetween(requester, from, from.plusDays(1));
        if (generated >= dailyLimit) {
            throw new BeginnerGuideDailyLimitExceededException();
        }
        return requester;
    }

    private OffsetDateTime dayStart() {
        return Instant.now(clock)
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);
    }
}
