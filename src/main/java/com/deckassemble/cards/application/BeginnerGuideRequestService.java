package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideRepository;

/** Coordinates idempotent, quota-limited beginner guide requests. */
public class BeginnerGuideRequestService {
    private final BeginnerGuideRepository guideRepository;
    private final BeginnerGuideGenerationQuota quota;
    private final BeginnerGuideGenerationService generationService;

    public BeginnerGuideRequestService(
            BeginnerGuideRepository guideRepository,
            BeginnerGuideGenerationQuota quota,
            BeginnerGuideGenerationService generationService) {
        this.guideRepository = guideRepository;
        this.quota = quota;
        this.generationService = generationService;
    }

    public BeginnerGuide request(Long cardId) {
        var existing = guideRepository.findById(cardId);
        if (existing.isPresent()) {
            return existing.get();
        }
        var requester = quota.requireAvailable();
        return generationService.generate(cardId, requester);
    }
}
