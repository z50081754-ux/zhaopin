package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:research-mvc;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes"
})
@AutoConfigureMockMvc
class PublicResearchControllerTest {
    @Autowired MockMvc mvc;

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
    void preservesInvalidRequestCodeForExistingNonResearchValidation() throws Exception {
        mvc.perform(post("/api/admin/jobs")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    private void assertDenied(int statusCode) {
        assertTrue(Set.of(401, 403).contains(statusCode),
            () -> "Expected security denial but received HTTP " + statusCode);
    }
}
