package com.deckassemble.decks.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class CategoryTemplateControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldCreateTemplateWithOrderedItems() throws Exception {
        String subject = "auth0|template-crud";

        mockMvc.perform(
                        post("/category-templates")
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"Ramp Focus\",\"itemNames\":"
                                                + "[\"Ramp\",\"Removal\",\"Wincons\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.itemNames.length()").value(3))
                .andExpect(jsonPath("$.itemNames[0]").value("Ramp"))
                .andExpect(jsonPath("$.itemNames[2]").value("Wincons"));
    }

    @Test
    void shouldRejectCaseInsensitiveDuplicateTemplateName() throws Exception {
        String subject = "auth0|template-dup";
        createTemplate(subject, "Ramp Focus", "Ramp");

        mockMvc.perform(
                        post("/category-templates")
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"ramp focus\",\"itemNames\":[\"Ramp\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldUpdateTemplateReplacingItems() throws Exception {
        String subject = "auth0|template-update";
        long templateId = createTemplate(subject, "Ramp Focus", "Ramp");

        mockMvc.perform(
                        patch("/category-templates/{templateId}", templateId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"Ramp Focus\",\"itemNames\":"
                                                + "[\"Removal\",\"Wincons\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemNames.length()").value(2))
                .andExpect(jsonPath("$.itemNames[0]").value("Removal"));
    }

    @Test
    void shouldHideTemplatesCreatedByOtherProfiles() throws Exception {
        createTemplate("auth0|template-owner", "Private Template", "Item");

        mockMvc.perform(
                        get("/category-templates")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|template-other"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldDeleteTemplate() throws Exception {
        String subject = "auth0|template-delete";
        long templateId = createTemplate(subject, "Doomed Template", "Item");

        mockMvc.perform(
                        delete("/category-templates/{templateId}", templateId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/category-templates").with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long createTemplate(String subject, String name, String firstItemName)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/category-templates")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"%s\",\"itemNames\":[\"%s\"]}"
                                                        .formatted(name, firstItemName)))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
