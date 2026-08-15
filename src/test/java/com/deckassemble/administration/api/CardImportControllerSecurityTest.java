package com.deckassemble.administration.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.imports.application.CardImportTrigger;
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
    @MockitoBean private CardImportTrigger cardImportTrigger;
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
    void shouldAcceptImportForAdministrators() throws Exception {
        when(cardImportTrigger.trigger(
                        org.mockito.ArgumentMatchers.eq("set:mar"),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(7L);

        mockMvc.perform(
                        post("/admin/card-imports")
                                .queryParam("query", "set:mar")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.runId").value(7));
    }

    @Test
    void shouldCombineSeriesKeysIntoSingleQueryForAdministrators() throws Exception {
        when(cardImportTrigger.trigger(
                        org.mockito.ArgumentMatchers.eq("e:tmt,tmc,hob,hoc"),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(9L);

        mockMvc.perform(
                        post("/admin/card-imports")
                                .queryParam("series", "TMNT", "hobbit")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.runId").value(9));
    }

    @Test
    void shouldRejectImportWithUnknownSeriesKey() throws Exception {
        mockMvc.perform(
                        post("/admin/card-imports")
                                .queryParam("series", "POKEMON")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectImportWithBothQueryAndSeries() throws Exception {
        mockMvc.perform(
                        post("/admin/card-imports")
                                .queryParam("query", "set:mar")
                                .queryParam("series", "TMNT")
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectImportWithNeitherQueryNorSeries() throws Exception {
        mockMvc.perform(post("/admin/card-imports").with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectSeriesListWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/admin/card-imports/series")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidSeriesListForNonAdministrators() throws Exception {
        mockMvc.perform(get("/admin/card-imports/series").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnSeriesListWithoutQueryFragmentsForAdministrators() throws Exception {
        mockMvc.perform(get("/admin/card-imports/series").with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].key").value("MARVEL"))
                .andExpect(jsonPath("$[0].label").value("Marvel"))
                .andExpect(jsonPath("$[3].key").value("TMNT"))
                .andExpect(jsonPath("$[0].setCodes").doesNotExist());
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
