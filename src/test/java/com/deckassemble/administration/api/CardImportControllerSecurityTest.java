package com.deckassemble.administration.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.imports.application.CardImportService;
import com.deckassemble.imports.application.ImportResult;
import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.imports.domain.CardImportRun;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

class CardImportControllerSecurityTest extends AbstractIntegrationTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CardImportService cardImportService;
    @MockitoBean private ImportRunRecorder importRunRecorder;

    @Test
    void shouldRejectImportWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/admin/card-imports").queryParam("query", "set:mar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectHistoryWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/card-imports")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidHistoryForNonAdministrators() throws Exception {
        mockMvc.perform(get("/admin/card-imports").with(jwt())).andExpect(status().isForbidden());
    }

    @Test
    void shouldImportCardsForAdministrators() throws Exception {
        when(cardImportService.importQuery("set:mar")).thenReturn(new ImportResult(7L, 5, 3, 2, 0));

        mockMvc.perform(
                        post("/admin/card-imports")
                                .queryParam("query", "set:mar")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(7))
                .andExpect(jsonPath("$.recordsRead").value(5))
                .andExpect(jsonPath("$.recordsCreated").value(3))
                .andExpect(jsonPath("$.recordsUpdated").value(2))
                .andExpect(jsonPath("$.recordsFailed").value(0));
    }

    @Test
    void shouldReturnHistoryForAdministrators() throws Exception {
        CardImportRun run =
                new CardImportRun(
                        "scryfall",
                        "set:mar",
                        OffsetDateTime.parse("2026-07-19T20:00:00Z"),
                        "admin-sub");
        ReflectionTestUtils.setField(run, "id", 7L);
        run.complete(OffsetDateTime.parse("2026-07-19T20:05:00Z"));
        when(importRunRecorder.history()).thenReturn(List.of(run));

        mockMvc.perform(get("/admin/card-imports").with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("scryfall"))
                .andExpect(jsonPath("$[0].query").value("set:mar"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
