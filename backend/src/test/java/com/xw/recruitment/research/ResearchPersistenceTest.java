package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:research-persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.research.enabled=false"
})
class ResearchPersistenceTest {
    @Autowired ResearchCampaignRepository campaigns;
    @Autowired ResearchSubmissionRepository submissions;

    @Test void migrationCreatesPausedCampaignAndPersistsScenes() {
        assertEquals("PAUSED", campaigns.findById(1L).orElseThrow().getStatus());
        ResearchSubmissionEntity entity = new ResearchSubmissionEntity(
            "SP-20260828-ABC12345", "OPEN_CARD", 5, "SECURITY",
            "feedback", "cipher", "nonce", "wallet-hash", "ip-hash", "context-hash",
            "2026-08-01", Instant.parse("2026-08-28T00:00:00Z"),
            Instant.parse("2026-08-28T00:00:00Z"),
            Set.of("TRAVEL", "SHOPPING"));
        submissions.saveAndFlush(entity);
        assertEquals(Set.of("TRAVEL", "SHOPPING"), submissions.findById(entity.getId()).orElseThrow().getScenes());
        assertTrue(submissions.existsByWalletHash("wallet-hash"));
    }
}
