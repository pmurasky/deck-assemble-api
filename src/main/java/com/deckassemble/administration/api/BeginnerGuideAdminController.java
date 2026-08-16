package com.deckassemble.administration.api;

import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideDraft;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideStatus;
import com.deckassemble.cards.domain.CardRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/beginner-guides")
public class BeginnerGuideAdminController {

    private final BeginnerGuideRepository guideRepository;
    private final CardRepository cardRepository;

    public BeginnerGuideAdminController(
            BeginnerGuideRepository guideRepository, CardRepository cardRepository) {
        this.guideRepository = guideRepository;
        this.cardRepository = cardRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BeginnerGuidePageResponse list(
            @RequestParam(defaultValue = "DRAFT,STALE,REPORTED") Set<BeginnerGuideStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var guides = guideRepository.findByStatusIn(status, PageRequest.of(page, size));
        Map<Long, String> cardNames = cardNames(guides.getContent());
        List<BeginnerGuideAdminResponse> content =
                guides.stream()
                        .map(
                                guide ->
                                        response(
                                                guide,
                                                Objects.requireNonNull(
                                                        cardNames.get(guide.getCardId()))))
                        .toList();
        return new BeginnerGuidePageResponse(content, guides.getTotalElements());
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public BeginnerGuideAdminResponse edit(
            @PathVariable Long cardId, @Valid @RequestBody BeginnerGuideEditRequest request) {
        BeginnerGuide guide =
                guideRepository
                        .findById(cardId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        guide.replaceContent(
                new BeginnerGuideDraft(
                        request.summary(),
                        request.examples(),
                        request.whenToUse(),
                        guide.getSourceRulingsSnapshot(),
                        guide.getSourceOracleHash()));
        guideRepository.save(guide);
        String cardName =
                cardRepository
                        .findById(cardId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
                        .getName();
        return response(guide, cardName);
    }

    private Map<Long, String> cardNames(List<BeginnerGuide> guides) {
        List<Long> cardIds = guides.stream().map(BeginnerGuide::getCardId).toList();
        return cardRepository.findAllById(cardIds).stream()
                .collect(Collectors.toMap(card -> card.getId(), card -> card.getName()));
    }

    private static BeginnerGuideAdminResponse response(BeginnerGuide guide, String cardName) {
        return new BeginnerGuideAdminResponse(
                guide.getCardId(),
                cardName,
                guide.getStatus(),
                guide.getSummary(),
                guide.getExamples(),
                guide.getWhenToUse(),
                guide.getSourceRulingsSnapshot(),
                guide.getGeneratedAt(),
                guide.getReviewedBy());
    }

    public record BeginnerGuidePageResponse(
            List<BeginnerGuideAdminResponse> content, long totalElements) {}

    public record BeginnerGuideEditRequest(
            @NotBlank String summary, @NotBlank String examples, @NotBlank String whenToUse) {}

    public record BeginnerGuideAdminResponse(
            Long cardId,
            String cardName,
            BeginnerGuideStatus status,
            String summary,
            String examples,
            String whenToUse,
            String sourceRulingsSnapshot,
            OffsetDateTime generatedAt,
            @Nullable String reviewedBy) {}
}
