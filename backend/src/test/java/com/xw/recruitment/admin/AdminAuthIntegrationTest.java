package com.xw.recruitment.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:admin-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes",
    "xw.admin.account=restored.operator",
    "xw.admin.password=session-secret"
})
@AutoConfigureMockMvc
class AdminAuthIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void returnsUnauthorizedForSessionProbeWithoutAnAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(get("/api/admin/session"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheExactPrincipalFromThePersistedLoginSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/admin/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"account\":\"restored.operator\",\"password\":\"session-secret\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);

        mockMvc.perform(get("/api/admin/session").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account").value("restored.operator"));
    }
}
