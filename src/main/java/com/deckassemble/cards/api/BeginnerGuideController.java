package com.deckassemble.cards.api;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideStatus;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/cards/{cardId}/beginner-guide")
@PreAuthorize("isAuthenticated()")
public class BeginnerGuideController {

    private final BeginnerGuideRepository guideRepository;

    public BeginnerGuideController(BeginnerGuideRepository guideRepository) {
        this.guideRepository = guideRepository;
    }

    @GetMapping
    public BeginnerGuideResponse get(@PathVariable long cardId) {
        var guide = guideRepository.findById(cardId).orElseThrow(BeginnerGuideController::notFound);
        if (!isVisible(guide.getStatus())) {
            throw notFound();
        }
        return BeginnerGuideResponse.from(guide);
    }

    private static boolean isVisible(BeginnerGuideStatus status) {
        return status == BeginnerGuideStatus.PUBLISHED || status == BeginnerGuideStatus.STALE;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Beginner guide not found");
    }

    public record BeginnerGuideResponse(
            Long cardId,
            BeginnerGuideStatus status,
            String summary,
            String examples,
            String whenToUse,
            @Nullable OffsetDateTime publishedAt) {

        private static BeginnerGuideResponse from(BeginnerGuide guide) {
            return new BeginnerGuideResponse(
                    guide.getCardId(),
                    guide.getStatus(),
                    guide.getSummary(),
                    guide.getExamples(),
                    guide.getWhenToUse(),
                    guide.getPublishedAt());
        }
    }
}
