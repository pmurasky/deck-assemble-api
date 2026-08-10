package com.deckassemble.community.api;

import com.deckassemble.community.domain.ModerationReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Files a report against a piece of content or a profile. */
public record ModerationReportRequest(
        @NotNull ModerationReport.ResourceType resourceType,
        @NotBlank @Size(max = 36) String resourceId,
        @NotNull ModerationReport.Reason reason,
        @Nullable @Size(max = 2000) String details) {}
