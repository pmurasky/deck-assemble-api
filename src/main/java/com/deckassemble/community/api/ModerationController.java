package com.deckassemble.community.api;

import com.deckassemble.community.application.ModerationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal moderation report workflow: any authenticated user files a report (starts {@code OPEN});
 * an admin resolves or dismisses it. No listing endpoint — not requested, and {@code
 * ModerationReportRepository} already exists for a future admin queue to query directly.
 */
@RestController
@RequestMapping("/community/reports")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    public ResponseEntity<ModerationReportResponse> create(
            @Valid @RequestBody ModerationReportRequest request) {
        ModerationReportResponse response =
                ModerationReportResponse.from(
                        moderationService.create(
                                request.resourceType(),
                                request.resourceId(),
                                request.reason(),
                                request.details()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ModerationReportResponse resolve(@PathVariable UUID reportId) {
        return ModerationReportResponse.from(moderationService.resolve(reportId));
    }

    @PostMapping("/{reportId}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    public ModerationReportResponse dismiss(@PathVariable UUID reportId) {
        return ModerationReportResponse.from(moderationService.dismiss(reportId));
    }
}
