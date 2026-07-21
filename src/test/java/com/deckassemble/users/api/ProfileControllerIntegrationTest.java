package com.deckassemble.users.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class ProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldCreateAndReturnCurrentProfile() throws Exception {
        mockMvc.perform(get("/profile").with(jwt().jwt(jwt -> jwt.subject("auth0|123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.displayName").value("auth0|123"));
    }

    @Test
    void shouldUpdateCurrentProfile() throws Exception {
        mockMvc.perform(
                        patch("/profile")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|456")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "displayName": "Player One",
                      "preferredFormat": "COMMANDER"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Player One"))
                .andExpect(jsonPath("$.preferredFormat").value("COMMANDER"));
    }

    @Test
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/profile")).andExpect(status().isUnauthorized());
    }
}
