package com.xw.recruitment.research;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
    "spring.datasource.url=jdbc:h2:mem:research-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes",
    "xw.research.enabled=false",
    "xw.research.wallet-encryption-key=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
    "xw.research.wallet-hash-key=wallet-hash-disabled-admin-key-000001",
    "xw.research.privacy-hash-key=privacy-hash-disabled-admin-key-0001"
})
@AutoConfigureMockMvc
class ResearchDisabledIntegrationTest {
    private static final String ENCRYPTION_KEY =
        "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";
    private static final String WALLET_HASH_KEY =
        "wallet-hash-disabled-admin-key-000001";
    private static final String PRIVACY_HASH_KEY =
        "privacy-hash-disabled-admin-key-0001";
    private static final String WALLET = "TJRabPrwbZy45sbavfcjinPJC18kjpRTv8";

    @Autowired MockMvc mockMvc;
    @Autowired ResearchSubmissionRepository submissions;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seedEncryptedSubmissionAndStoredActiveCampaign() {
        submissions.deleteAll();
        jdbc.update("UPDATE research_campaign SET status = 'ACTIVE', terms_version = '2026-08-01' WHERE id = 1");
        ResearchCryptoService seeder = new ResearchCryptoService(
            true, ENCRYPTION_KEY, WALLET_HASH_KEY, PRIVACY_HASH_KEY);
        ResearchCryptoService.EncryptedWallet encrypted = seeder.encryptWallet(WALLET);
        Instant now = Instant.parse("2026-08-28T00:00:00Z");
        submissions.saveAndFlush(new ResearchSubmissionEntity(
            "SP-20260828-DISABLED", "OPEN_CARD", 5, "SECURITY", "existing data",
            encrypted.ciphertext(), encrypted.nonce(), seeder.walletHash(WALLET),
            seeder.privacyHash("198.51.100.50"), seeder.privacyHash("test-agent"),
            "2026-08-01", now, now, Set.of("TRAVEL")));
    }

    @Test
    void separatesStoredCampaignStateFromTheDisabledPublicIntake() throws Exception {
        mockMvc.perform(get("/api/research/campaign"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(get("/api/admin/research/campaign")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.effectiveStatus").value("DISABLED"))
            .andExpect(jsonPath("$.intakeEnabled").value(false))
            .andExpect(jsonPath("$.dataAvailable").value(true));
    }

    @Test
    void allowsPausingButRejectsActivationWhileIntakeIsDisabled() throws Exception {
        mockMvc.perform(put("/api/admin/research/campaign")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAUSED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAUSED"))
            .andExpect(jsonPath("$.effectiveStatus").value("DISABLED"));

        mockMvc.perform(put("/api/admin/research/campaign")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESEARCH_INTAKE_DISABLED"));
        assertEquals("PAUSED", jdbc.queryForObject(
            "SELECT status FROM research_campaign WHERE id = 1", String.class));
    }

    @Test
    void keepsExistingEncryptedAdminDataManageableWithSecretsSupplied() throws Exception {
        long id = submissions.findAll().getFirst().getId();

        mockMvc.perform(get("/api/admin/research/submissions")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submissions[0].maskedWalletAddress")
                .value("TJRabP••••••pRTv8"))
            .andExpect(content().string(not(containsString(WALLET))));

        mockMvc.perform(get("/api/admin/research/submissions/{id}", id)
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.walletAddress").value(WALLET));

        mockMvc.perform(post("/api/admin/research/submissions/lookup")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"walletAddress\":\"" + WALLET + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.walletAddress").value(WALLET));

        mockMvc.perform(get("/api/admin/research/submissions/export")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(WALLET)));
    }
}
