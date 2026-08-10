package com.deckassemble.community.api;

import com.deckassemble.community.domain.ModerationReport;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ModerationReportResponse(
        UUID id,
        @Nullable Long reporterId,
        ModerationReport.ResourceType resourceType,
        String resourceId,
        ModerationReport.Reason reason,
        @Nullable String details,
        ModerationReport.Status status,
        Instant createdAt) {

    public static ModerationReportResponse from(ModerationReport report) {
        return new ModerationReportResponse(
                report.getId(),
                report.getReporterId(),
                report.getResourceType(),
                report.getResourceId(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getCreatedAt());
    }
}
