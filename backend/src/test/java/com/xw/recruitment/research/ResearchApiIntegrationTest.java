package com.xw.recruitment.research;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:research-admin-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes",
    "xw.research.enabled=true",
    "xw.research.wallet-encryption-key=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
    "xw.research.wallet-hash-key=wallet-hash-admin-test-key",
    "xw.research.privacy-hash-key=privacy-hash-admin-test-key",
    "xw.research.rate-limit-per-minute=20"
})
@AutoConfigureMockMvc
class ResearchApiIntegrationTest {
    private static final String WALLET_ONE = "TJRabPrwbZy45sbavfcjinPJC18kjpRTv8";
    private static final String WALLET_TWO = "TXLAQ63Xg1NAzckPwKHvzw7CSEmLMEqcdj";
    private static final String WALLET_THREE = "T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb";

    @Autowired MockMvc mockMvc;
    @Autowired ResearchSubmissionService service;
    @Autowired ResearchSubmissionRepository submissions;
    @Autowired JdbcTemplate jdbc;

    private List<ResearchSubmissionEntity> seeded;

    @BeforeEach
    void seedThreeSubmissionsThroughPublicService() {
        submissions.deleteAll();
        jdbc.update("UPDATE research_campaign SET status = 'ACTIVE', terms_version = '2026-08-01' WHERE id = 1");

        service.submit(request("OPEN_CARD", 5, Set.of("TRAVEL", "SHOPPING"),
            "SECURITY", "透明，快速\n到账", WALLET_ONE), "192.0.2.1", "admin-api-test-1");
        service.submit(request("APP_DOWNLOAD", 4, Set.of("GAMING"),
            "FEES", "=SUM(1,2) \"清晰\"", WALLET_TWO), "192.0.2.2", "admin-api-test-2");
        service.submit(request("FREE_CARD", 3, Set.of("ATM"),
            "SPEED", "Good", WALLET_THREE), "192.0.2.3", "admin-api-test-3");

        seeded = submissions.findAll().stream()
            .sorted(Comparator.comparing(ResearchSubmissionEntity::getId))
            .toList();
    }

    @Test
    void protectsAdminSummaryAndReturnsAggregates() throws Exception {
        mockMvc.perform(get("/api/admin/research/summary"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/research/summary")
                .with(user("staff").roles("USER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/research/summary")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.averageRating").value(4.0))
            .andExpect(jsonPath("$.ratingDistribution.5").value(1))
            .andExpect(jsonPath("$.sceneDistribution.TRAVEL").value(1))
            .andExpect(jsonPath("$.concernDistribution.FEES").value(1))
            .andExpect(jsonPath("$.sourceDistribution.APP_DOWNLOAD").value(1));
    }

    @Test
    void filtersAndPaginatesMaskedListItems() throws Exception {
        String numberFragment = seeded.get(1).getSubmissionNumber().substring(3, 11);

        mockMvc.perform(get("/api/admin/research/submissions")
                .with(user("admin").roles("ADMIN"))
                .param("number", numberFragment)
                .param("rating", "4")
                .param("concern", "FEES")
                .param("source", "APP_DOWNLOAD")
                .param("scene", "GAMING")
                .param("from", "2020-01-01")
                .param("to", "2100-01-01")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.pages").value(1))
            .andExpect(jsonPath("$.submissions[0].id").value(seeded.get(1).getId()))
            .andExpect(jsonPath("$.submissions[0].maskedWalletAddress").value("TXLAQ6••••••Eqcdj"))
            .andExpect(content().string(not(containsString(WALLET_TWO))));
    }

    @Test
    void decryptsWalletOnlyForProtectedDetailAndExactLookup() throws Exception {
        mockMvc.perform(get("/api/admin/research/submissions/{id}", seeded.get(0).getId())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.walletAddress").value(WALLET_ONE));

        mockMvc.perform(post("/api/admin/research/submissions/lookup")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"walletAddress\":\"" + WALLET_ONE + "\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.walletAddress").value(WALLET_ONE));
    }

    @Test
    void exportsBomPrefixedRfc4180CsvWithoutCaching() throws Exception {
        mockMvc.perform(get("/api/admin/research/submissions/export")
                .with(user("admin").roles("ADMIN"))
                .param("source", "APP_DOWNLOAD"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=web3-wallet-research.csv"))
            .andExpect(content().contentType("text/csv;charset=UTF-8"))
            .andExpect(content().string(containsString("\uFEFFsubmission number,source,rating,scenes,concern,feedback,network,full wallet address,terms version,consent time,created time")))
            .andExpect(content().string(containsString("\"'=SUM(1,2) \"\"清晰\"\"\"")))
            .andExpect(content().string(containsString(WALLET_TWO)))
            .andExpect(content().string(not(containsString(WALLET_ONE))));
    }

    @Test
    void deletesSingleAndDistinctBatchIds() throws Exception {
        mockMvc.perform(delete("/api/admin/research/submissions/{id}", seeded.get(2).getId())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
        assertFalse(submissions.existsById(seeded.get(2).getId()));
        assertTrue(submissions.existsById(seeded.get(0).getId()));
        assertTrue(submissions.existsById(seeded.get(1).getId()));

        long first = seeded.get(0).getId();
        long second = seeded.get(1).getId();
        mockMvc.perform(delete("/api/admin/research/submissions/batch")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[" + first + "," + second + "," + first + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deleted").value(2));
    }

    @Test
    void rejectsInvalidOrOversizedBatchBeforeDeletingAnything() throws Exception {
        long first = seeded.get(0).getId();
        long second = seeded.get(1).getId();

        mockMvc.perform(delete("/api/admin/research/submissions/batch")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[" + first + ",0," + second + "]}"))
            .andExpect(status().isBadRequest());
        assertTrue(submissions.existsById(first));
        assertTrue(submissions.existsById(second));

        String oversizedIds = java.util.stream.LongStream.rangeClosed(1, 101)
            .mapToObj(Long::toString)
            .collect(java.util.stream.Collectors.joining(","));
        mockMvc.perform(delete("/api/admin/research/submissions/batch")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[" + oversizedIds + "]}"))
            .andExpect(status().isBadRequest());
        assertTrue(submissions.existsById(first));
        assertTrue(submissions.existsById(second));
    }

    @Test
    void readsAndUpdatesCampaignWithoutChangingTermsVersion() throws Exception {
        jdbc.update("UPDATE research_campaign SET updated_at = ? WHERE id = 1",
            Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/admin/research/campaign")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.termsVersion").value("2026-08-01"));

        mockMvc.perform(put("/api/admin/research/campaign")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.termsVersion").value("2026-08-01"))
            .andExpect(jsonPath("$.updatedAt").value(not("2020-01-01T00:00:00Z")));
    }

    private ResearchSubmissionRequest request(String source, int rating, Set<String> scenes,
            String concern, String feedback, String walletAddress) {
        return new ResearchSubmissionRequest(source, rating, scenes, concern, feedback,
            "TRC20", walletAddress, "2026-08-01", true);
    }
}
