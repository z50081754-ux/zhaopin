package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:research-mvc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes",
    "xw.cors.allowed-origins=https://public.example",
    "xw.cors.admin-origins=https://admin.example"
})
@AutoConfigureMockMvc
class PublicResearchControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetCampaign() {
        jdbc.update("UPDATE research_campaign SET status = 'PAUSED' WHERE id = 1");
    }

    @Test
    void permitsOnlyIntendedPublicResearchMethodsAndPaths() throws Exception {
        mvc.perform(get("/api/research/campaign"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("web3钱包产品调研"));

        assertDenied(mvc.perform(post("/api/research/campaign"))
            .andReturn().getResponse().getStatus());
        assertDenied(mvc.perform(get("/api/research/submissions"))
            .andReturn().getResponse().getStatus());
        assertDenied(mvc.perform(get("/api/research/internal"))
            .andReturn().getResponse().getStatus());
    }

    @Test
    void returnsStableValidationCodeForPublicResearchRequest() throws Exception {
        mvc.perform(post("/api/research/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void reportsDisabledInsteadOfMisreportingAStoredActiveCampaign() throws Exception {
        jdbc.update("UPDATE research_campaign SET status = 'ACTIVE' WHERE id = 1");

        mvc.perform(get("/api/research/campaign"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void exposesUnavailableAdminDataStateWhenNoCryptoSecretsAreConfigured() throws Exception {
        mvc.perform(get("/api/admin/research/campaign")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAUSED"))
            .andExpect(jsonPath("$.effectiveStatus").value("DISABLED"))
            .andExpect(jsonPath("$.intakeEnabled").value(false))
            .andExpect(jsonPath("$.dataAvailable").value(false));

        mvc.perform(get("/api/admin/research/submissions")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("RESEARCH_DATA_UNAVAILABLE"));
    }

    @Test
    void returnsStableValidationCodeForUnreadablePublicResearchBodies() throws Exception {
        mvc.perform(post("/api/research/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mvc.perform(post("/api/research/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":\"five\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mvc.perform(post("/api/research/submissions")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsNonJsonPublicResearchBodiesWithStableValidationCode() throws Exception {
        mvc.perform(post("/api/research/submissions")
                .contentType(MediaType.TEXT_PLAIN)
                .content("not-json"))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void appliesLeastPrivilegeCorsPerResearchAdminAndExistingRoutes() throws Exception {
        mvc.perform(options("/api/research/submissions")
                .header("Origin", "https://public.example")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://public.example"))
            .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));

        mvc.perform(options("/api/admin/research/campaign")
                .header("Origin", "https://admin.example")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://admin.example"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mvc.perform(options("/api/applications")
                .header("Origin", "https://public.example")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://public.example"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void rejectsCrossPolicyOriginsMutationMethodsAndWildcardOrigins() throws Exception {
        mvc.perform(options("/api/admin/research/campaign")
                .header("Origin", "https://public.example")
                .header("Access-Control-Request-Method", "PUT"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mvc.perform(options("/api/research/submissions")
                .header("Origin", "https://public.example")
                .header("Access-Control-Request-Method", "DELETE"))
            .andExpect(status().isForbidden());

        mvc.perform(options("/api/admin/research/campaign")
                .header("Origin", "https://attacker.trycloudflare.com")
                .header("Access-Control-Request-Method", "PUT"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void preservesInvalidRequestCodeForExistingNonResearchValidation() throws Exception {
        mvc.perform(post("/api/admin/jobs")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        var unsupported = mvc.perform(post("/api/admin/jobs")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.TEXT_PLAIN)
                .content("not-json"))
            .andExpect(status().isUnsupportedMediaType())
            .andReturn().getResponse().getContentAsString();
        assertFalse(unsupported.contains("VALIDATION_FAILED"));
    }

    private void assertDenied(int statusCode) {
        assertTrue(Set.of(401, 403).contains(statusCode),
            () -> "Expected security denial but received HTTP " + statusCode);
    }
}
