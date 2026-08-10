package com.deckassemble.community.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class ModerationControllerIntegrationTest extends AbstractIntegrationTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":\"([^\"]+)\"");

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldFileAnOpenReportForAnAuthenticatedUser() throws Exception {
        mockMvc.perform(
                        post("/community/reports")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|reporter-happy")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"resourceType\":\"COMMENT\",\"resourceId\":\"abc-123\","
                                                + "\"reason\":\"SPAM\",\"details\":\"looks like spam\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.resourceType").value("COMMENT"))
                .andExpect(jsonPath("$.reason").value("SPAM"));
    }

    @Test
    void shouldRejectAnonymousReportFiling() throws Exception {
        mockMvc.perform(
                        post("/community/reports")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"resourceType\":\"DECK\",\"resourceId\":\"1\","
                                                + "\"reason\":\"OTHER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAReportMissingRequiredFields() throws Exception {
        mockMvc.perform(
                        post("/community/reports")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|reporter-invalid")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"resourceType\":\"DECK\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLetAnAdminResolveAReport() throws Exception {
        String reportId = fileReport("auth0|reporter-resolve");

        mockMvc.perform(
                        post("/community/reports/{id}/resolve", reportId)
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void shouldLetAnAdminDismissAReport() throws Exception {
        String reportId = fileReport("auth0|reporter-dismiss");

        mockMvc.perform(
                        post("/community/reports/{id}/dismiss", reportId)
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));
    }

    @Test
    void shouldForbidNonAdminsFromResolvingAReport() throws Exception {
        String reportId = fileReport("auth0|reporter-forbid");

        mockMvc.perform(
                        post("/community/reports/{id}/resolve", reportId)
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|not-an-admin"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenResolvingAnUnknownReport() throws Exception {
        mockMvc.perform(
                        post("/community/reports/{id}/resolve", UUID.randomUUID())
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isNotFound());
    }

    private String fileReport(String subject) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/community/reports")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"resourceType\":\"DECK\",\"resourceId\":\"1\","
                                                        + "\"reason\":\"OTHER\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        Matcher matcher = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        matcher.find();
        return matcher.group(1);
    }
}
