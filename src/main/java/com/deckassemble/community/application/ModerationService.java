package com.deckassemble.community.application;

import com.deckassemble.community.domain.ModerationReport;
import com.deckassemble.community.domain.ModerationReportRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Files moderation reports and drives their {@code OPEN -> RESOLVED|DISMISSED} lifecycle. */
@Service
@Transactional
public class ModerationService {

    private final ModerationReportRepository moderationReportRepository;
    private final CurrentUser currentUser;
    private final ProfileService profileService;

    public ModerationService(
            ModerationReportRepository moderationReportRepository,
            CurrentUser currentUser,
            ProfileService profileService) {
        this.moderationReportRepository = moderationReportRepository;
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    public ModerationReport create(
            ModerationReport.ResourceType resourceType,
            String resourceId,
            ModerationReport.Reason reason,
            @Nullable String details) {
        long reporterId = currentProfileId();
        return moderationReportRepository.save(
                new ModerationReport(reporterId, resourceType, resourceId, reason, details));
    }

    public ModerationReport resolve(UUID reportId) {
        ModerationReport report = reportFor(reportId);
        report.resolve();
        return moderationReportRepository.save(report);
    }

    public ModerationReport dismiss(UUID reportId) {
        ModerationReport report = reportFor(reportId);
        report.dismiss();
        return moderationReportRepository.save(report);
    }

    private ModerationReport reportFor(UUID reportId) {
        return moderationReportRepository
                .findById(reportId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Report not found"));
    }

    private long currentProfileId() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }
}
