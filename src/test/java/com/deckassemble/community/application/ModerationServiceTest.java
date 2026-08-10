package com.deckassemble.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.deckassemble.community.domain.ModerationReport;
import com.deckassemble.community.domain.ModerationReportRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock private ModerationReportRepository moderationReportRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        service = new ModerationService(moderationReportRepository, currentUser, profileService);
    }

    private Profile profileWithId(long id) {
        Profile profile = new Profile("auth0|reporter", "reporter@example.com");
        ReflectionTestUtils.setField(profile, "id", id);
        return profile;
    }

    @Test
    void shouldCreateAnOpenReportAttributedToTheReportingProfile() {
        when(currentUser.subject()).thenReturn(Optional.of("auth0|reporter"));
        when(profileService.getOrCreate("auth0|reporter")).thenReturn(profileWithId(9L));
        when(moderationReportRepository.save(any(ModerationReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModerationReport report =
                service.create(
                        ModerationReport.ResourceType.COMMENT,
                        "some-comment-id",
                        ModerationReport.Reason.SPAM,
                        "looks like spam");

        assertThat(report.getReporterId()).isEqualTo(9L);
        assertThat(report.getStatus()).isEqualTo(ModerationReport.Status.OPEN);
        assertThat(report.getResourceType()).isEqualTo(ModerationReport.ResourceType.COMMENT);
    }

    @Test
    void shouldResolveAnOpenReport() {
        ModerationReport report =
                new ModerationReport(
                        9L,
                        ModerationReport.ResourceType.DECK,
                        "42",
                        ModerationReport.Reason.OTHER,
                        null);
        UUID reportId = UUID.randomUUID();
        when(moderationReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(moderationReportRepository.save(any(ModerationReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModerationReport resolved = service.resolve(reportId);

        assertThat(resolved.getStatus()).isEqualTo(ModerationReport.Status.RESOLVED);
    }

    @Test
    void shouldDismissAnOpenReport() {
        ModerationReport report =
                new ModerationReport(
                        9L,
                        ModerationReport.ResourceType.PROFILE,
                        "3",
                        ModerationReport.Reason.HARASSMENT,
                        null);
        UUID reportId = UUID.randomUUID();
        when(moderationReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(moderationReportRepository.save(any(ModerationReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModerationReport dismissed = service.dismiss(reportId);

        assertThat(dismissed.getStatus()).isEqualTo(ModerationReport.Status.DISMISSED);
    }

    @Test
    void shouldReturnNotFoundWhenResolvingAnUnknownReport() {
        UUID reportId = UUID.randomUUID();
        when(moderationReportRepository.findById(reportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(reportId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
