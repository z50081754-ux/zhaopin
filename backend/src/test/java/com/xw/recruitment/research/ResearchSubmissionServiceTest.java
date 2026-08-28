package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:research-service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.research.enabled=true",
    "xw.research.wallet-encryption-key=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
    "xw.research.wallet-hash-key=wallet-hash-test-key",
    "xw.research.privacy-hash-key=privacy-hash-test-key",
    "xw.research.rate-limit-per-minute=2"
})
class ResearchSubmissionServiceTest {
    private static final String WALLET = "TJRabPrwbZy45sbavfcjinPJC18kjpRTv8";

    @Autowired ResearchSubmissionService service;
    @Autowired ResearchSubmissionRepository submissions;
    @Autowired ResearchCryptoService crypto;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetCampaignAndSubmissions() {
        submissions.deleteAll();
        jdbc.update("UPDATE research_campaign SET status = 'ACTIVE', terms_version = '2026-08-01' WHERE id = 1");
    }

    @Test
    void acceptsSubmissionOnlyAfterEncryptedRecordIsPersisted() {
        var before = Instant.now();

        var saved = service.submit(validRequest(), "127.0.0.1", "test-agent");

        assertTrue(saved.submissionNumber().startsWith("SP-"));
        assertEquals("TJRabP••••••pRTv8", saved.maskedWalletAddress());
        assertFalse(saved.submittedAt().isBefore(before));
        var entity = submissions.findBySubmissionNumber(saved.submissionNumber()).orElseThrow();
        assertEquals("TRC20", entity.getWalletNetwork());
        assertEquals("2026-08-01", entity.getTermsVersion());
        assertEquals(Set.of("TRAVEL", "SHOPPING"), entity.getScenes());
        assertEquals(WALLET, crypto.decryptWallet(new ResearchCryptoService.EncryptedWallet(
            entity.getWalletCiphertext(), entity.getWalletNonce())));
        assertNotEquals(WALLET, entity.getWalletCiphertext());
        assertEquals(64, entity.getWalletHash().length());
        assertEquals(64, entity.getIpHash().length());
        assertEquals(64, entity.getRequestContextHash().length());
        assertFalse(entity.getIpHash().contains("127.0.0.1"));
        assertFalse(entity.getRequestContextHash().contains("test-agent"));
        assertNotNull(entity.getConsentedAt());
    }

    @Test
    void rejectsNormalizedWalletThatWasAlreadySubmitted() {
        service.submit(validRequest(), "127.0.0.2", "first-agent");
        var withWhitespace = new ResearchSubmissionRequest("OPEN_CARD", 5,
            Set.of("TRAVEL", "SHOPPING"), "SECURITY", "费用需要透明。",
            "TRC20", "  " + WALLET + "  ", "2026-08-01", true);

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(withWhitespace, "127.0.0.3", "other-agent"));

        assertEquals("DUPLICATE_WALLET", exception.code());
        assertEquals(1, submissions.count());
    }

    @Test
    void rejectsTermsVersionThatIsNoLongerCurrent() {
        var stale = requestWith("OPEN_CARD", "TRC20", WALLET, "2026-07-01", true);

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(stale, "127.0.0.4", "test-agent"));

        assertEquals("TERMS_VERSION_MISMATCH", exception.code());
        assertEquals(0, submissions.count());
    }

    @Test
    void rejectsSubmissionWhileCampaignIsPaused() {
        jdbc.update("UPDATE research_campaign SET status = 'PAUSED' WHERE id = 1");

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(validRequest(), "127.0.0.5", "test-agent"));

        assertEquals("CAMPAIGN_PAUSED", exception.code());
    }

    @Test
    void rejectsInvalidTrc20Address() {
        var invalid = requestWith("OPEN_CARD", "TRC20",
            "TJRabPrwbZy45sbavfcjinPJC18kjpRTv7", "2026-08-01", true);

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(invalid, "127.0.0.6", "test-agent"));

        assertEquals("INVALID_TRC20_ADDRESS", exception.code());
    }

    @Test
    void rejectsValuesOutsideFixedAllowlistsAndMissingConsent() {
        var unsupported = requestWith("REFERRAL", "ERC20", WALLET, "2026-08-01", false);

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(unsupported, "127.0.0.7", "test-agent"));

        assertEquals("VALIDATION_FAILED", exception.code());
    }

    @Test
    void rejectsNullElementInsideScenesAsValidationFailure() {
        Set<String> malformedScenes = new HashSet<>();
        malformedScenes.add("TRAVEL");
        malformedScenes.add(null);
        var malformed = new ResearchSubmissionRequest("OPEN_CARD", 5, malformedScenes,
            "SECURITY", "费用需要透明。", "TRC20", WALLET, "2026-08-01", true);

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(malformed, "127.0.0.9", "test-agent"));

        assertEquals("VALIDATION_FAILED", exception.code());
    }

    @Test
    void rejectsThirdAttemptForSameHashedIpWithinOneMinute() {
        service.submit(validRequest(), "127.0.0.8", "test-agent");
        assertEquals("DUPLICATE_WALLET", assertThrows(ResearchApiException.class,
            () -> service.submit(validRequest(), "127.0.0.8", "test-agent")).code());

        var exception = assertThrows(ResearchApiException.class,
            () -> service.submit(validRequest(), "127.0.0.8", "test-agent"));

        assertEquals("RATE_LIMITED", exception.code());
    }

    private ResearchSubmissionRequest validRequest() {
        return new ResearchSubmissionRequest("OPEN_CARD", 5,
            Set.of("TRAVEL", "SHOPPING"), "SECURITY", "费用需要透明。",
            "TRC20", WALLET, "2026-08-01", true);
    }

    private ResearchSubmissionRequest requestWith(String source, String network, String wallet,
            String termsVersion, boolean consent) {
        return new ResearchSubmissionRequest(source, 5, Set.of("TRAVEL", "SHOPPING"),
            "SECURITY", "费用需要透明。", network, wallet, termsVersion, consent);
    }
}
