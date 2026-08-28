package com.xw.recruitment.research;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:research-client-identity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes",
    "xw.research.enabled=true",
    "xw.research.wallet-encryption-key=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
    "xw.research.wallet-hash-key=wallet-hash-client-identity-test-key",
    "xw.research.privacy-hash-key=privacy-hash-client-identity-test-key",
    "xw.research.rate-limit-per-minute=1",
    "xw.research.trusted-proxies=127.0.0.1,::1"
})
@AutoConfigureMockMvc
class ResearchClientIdentityIntegrationTest {
    private static final String WALLET_ONE = "TJRabPrwbZy45sbavfcjinPJC18kjpRTv8";
    private static final String WALLET_TWO = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj";

    @Autowired MockMvc mockMvc;
    @Autowired ResearchSubmissionRepository submissions;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetData() {
        submissions.deleteAll();
        jdbc.update("UPDATE research_campaign SET status = 'ACTIVE', terms_version = '2026-08-01' WHERE id = 1");
    }

    @Test
    void ignoresForwardingHeadersFromAnUntrustedDirectClient() throws Exception {
        mockMvc.perform(submission(WALLET_ONE, "198.51.100.10")
                .header("CF-Connecting-IP", "203.0.113.10")
                .header("X-Forwarded-For", "203.0.113.20")
                .header("X-Real-IP", "203.0.113.30"))
            .andExpect(status().isCreated());

        mockMvc.perform(submission(WALLET_TWO, "198.51.100.10")
                .header("CF-Connecting-IP", "203.0.113.11")
                .header("X-Forwarded-For", "203.0.113.21")
                .header("X-Real-IP", "203.0.113.31"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void selectsTheFirstUntrustedHopFromTheRightOfATrustedProxyChain() throws Exception {
        mockMvc.perform(submission(WALLET_ONE, "127.0.0.1")
                .header("X-Forwarded-For", "203.0.113.40, 198.51.100.20"))
            .andExpect(status().isCreated());

        mockMvc.perform(submission(WALLET_TWO, "127.0.0.1")
                .header("X-Forwarded-For", "203.0.113.41, 198.51.100.20"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void distinguishesClientsForwardedByTheDocumentedTrustedProxy() throws Exception {
        mockMvc.perform(submission(WALLET_ONE, "127.0.0.1")
                .header("X-Forwarded-For", "198.51.100.30"))
            .andExpect(status().isCreated());

        mockMvc.perform(submission(WALLET_TWO, "127.0.0.1")
                .header("X-Forwarded-For", "198.51.100.31"))
            .andExpect(status().isCreated());
    }

    private MockHttpServletRequestBuilder submission(String wallet, String remoteAddress) {
        return post("/api/research/submissions")
            .with(request -> {
                request.setRemoteAddr(remoteAddress);
                return request;
            })
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "source":"OPEN_CARD",
                  "rating":5,
                  "scenes":["TRAVEL"],
                  "concern":"SECURITY",
                  "feedback":"",
                  "walletNetwork":"TRC20",
                  "walletAddress":"%s",
                  "termsVersion":"2026-08-01",
                  "consent":true
                }
                """.formatted(wallet));
    }
}
