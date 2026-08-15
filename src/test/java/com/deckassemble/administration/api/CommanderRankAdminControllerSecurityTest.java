package com.deckassemble.administration.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.recommendations.application.CommanderRankRunRecorder;
import com.deckassemble.recommendations.application.CommanderRankService;
import com.deckassemble.recommendations.application.RefreshOutcome;
import com.deckassemble.recommendations.domain.CommanderRankRefreshRun;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

class CommanderRankAdminControllerSecurityTest extends AbstractIntegrationTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CommanderRankService commanderRankService;

    @MockitoBean private CommanderRankRunRecorder runRecorder;

    @Test
    void refreshRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/admin/commander-ranks/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void latestRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/commander-ranks/latest")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRequiresAdminRole() throws Exception {
        mockMvc.perform(post("/admin/commander-ranks/refresh").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void latestRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/commander-ranks/latest").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshReturnsOutcomeForAdmin() throws Exception {
        when(commanderRankService.refreshNow(anyString()))
                .thenReturn(RefreshOutcome.completed(123));

        mockMvc.perform(
                        post("/admin/commander-ranks/refresh")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardsUpdated").value(123));
    }

    @Test
    void refreshReturnsBadGatewayWhenFetchFails() throws Exception {
        when(commanderRankService.refreshNow(anyString()))
                .thenReturn(RefreshOutcome.failed("EDHREC returned no top commanders"));

        mockMvc.perform(
                        post("/admin/commander-ranks/refresh")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorSummary").value("EDHREC returned no top commanders"));
    }

    @Test
    void latestReturnsCompletedRunForAdmin() throws Exception {
        CommanderRankRefreshRun run =
                new CommanderRankRefreshRun(OffsetDateTime.parse("2026-02-10T10:15:30Z"), "manual");
        ReflectionTestUtils.setField(run, "id", 7L);
        run.complete(OffsetDateTime.parse("2026-02-10T10:16:00Z"), 42);
        when(runRecorder.latestCompleted()).thenReturn(Optional.of(run));

        mockMvc.perform(
                        get("/admin/commander-ranks/latest")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cardsUpdated").value(42))
                .andExpect(jsonPath("$.triggeredBy").value("manual"));
    }

    @Test
    void latestReturnsNotFoundWhenNoCompletedRun() throws Exception {
        when(runRecorder.latestCompleted()).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/admin/commander-ranks/latest")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isNotFound());
    }
}
