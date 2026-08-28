package com.xw.recruitment;

import com.xw.recruitment.visit.WebsiteVisitRepository;
import com.xw.recruitment.visit.WebsiteVisitService;
import com.xw.recruitment.visit.VisitSystem;
import com.xw.recruitment.site.SiteSettingsRepository;
import com.xw.recruitment.site.SiteSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:xw-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes"
})
class RecruitmentApplicationTests {
    @Autowired
    private WebsiteVisitService websiteVisitService;

    @Autowired
    private WebsiteVisitRepository websiteVisitRepository;

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Autowired
    private SiteSettingsRepository siteSettingsRepository;

    @BeforeEach
    void clearVisits() {
        websiteVisitRepository.deleteAll();
        siteSettingsRepository.deleteAll();
    }

    @Test
    void contextLoads() {}

    @Test
    void tracksVisitsAtFifteenSecondsAndFiltersByMinimumDuration() {
        WebsiteVisitService.VisitRequest fifteenSecondVisit = visit("visit-000000000001", 15);
        WebsiteVisitService.VisitRequest thirtySecondVisit = visit("visit-000000000002", 30);

        assertTrue(websiteVisitService.qualify(fifteenSecondVisit, "127.0.0.1").tracked());
        assertTrue(websiteVisitService.qualify(thirtySecondVisit, "127.0.0.1").tracked());
        assertEquals(2, websiteVisitRepository.count());

        var filtered = websiteVisitService.list(0, 20, 20, false);
        assertEquals(1, filtered.getTotalElements());
        assertEquals(30, filtered.getContent().getFirst().getDurationSeconds());
    }

    @Test
    void isolatesVisitsBySubsystemAndKeepsQueryFlagMonotonic() {
        websiteVisitService.qualify(VisitSystem.RECRUITMENT,
            visit("visit-recruitment-0001", 15, false), "127.0.0.1");
        websiteVisitService.qualify(VisitSystem.WALLETCHECK,
            visit("visit-walletcheck-0001", 15, false), "127.0.0.2");

        websiteVisitService.heartbeat(VisitSystem.WALLETCHECK, "visit-walletcheck-0001",
            new WebsiteVisitService.HeartbeatRequest(16, "/wallet/:address", true));
        websiteVisitService.heartbeat(VisitSystem.WALLETCHECK, "visit-walletcheck-0001",
            new WebsiteVisitService.HeartbeatRequest(17, "/wallet/:address", false));

        assertEquals(1, websiteVisitService.list(VisitSystem.RECRUITMENT, 0, 20, 0, false).getTotalElements());
        var wallet = websiteVisitService.list(VisitSystem.WALLETCHECK, 0, 20, 0, false).getContent().getFirst();
        assertEquals("walletcheck", wallet.getSystemCode());
        assertTrue(wallet.isQueriedAddress());
        assertEquals(17, wallet.getDurationSeconds());
    }

    @Test
    void rejectsUnknownSubsystemCode() {
        assertThrows(IllegalArgumentException.class, () -> VisitSystem.fromCode("other"));
    }

    @Test
    void rejectsHeartbeatForDifferentSubsystem() {
        websiteVisitService.qualify(VisitSystem.RECRUITMENT,
            visit("visit-recruitment-0002", 15, false), "127.0.0.1");

        assertThrows(IllegalArgumentException.class, () -> websiteVisitService.heartbeat(
            VisitSystem.WALLETCHECK,
            "visit-recruitment-0002",
            new WebsiteVisitService.HeartbeatRequest(16, "/wallet/:address", false)
        ));
    }

    @Test
    void rejectsQualificationForDifferentSubsystemWithoutMutatingVisit() {
        String visitId = "visit-recruitment-0003";
        websiteVisitService.qualify(VisitSystem.RECRUITMENT,
            visit(visitId, 15, false, "/jobs", "/jobs/123"), "127.0.0.1");

        assertThrows(IllegalArgumentException.class, () -> websiteVisitService.qualify(
            VisitSystem.WALLETCHECK,
            visit(visitId, 30, true, "/wallet/0x123", "/wallet/0x123"),
            "127.0.0.2"
        ));

        var stored = websiteVisitRepository.findByVisitId(visitId).orElseThrow();
        assertEquals("recruitment", stored.getSystemCode());
        assertEquals(15, stored.getDurationSeconds());
        assertEquals("/jobs/123", stored.getLastPath());
        assertFalse(stored.isQueriedAddress());
    }

    @Test
    void storesWalletPathsAsAddressTemplates() {
        String visitId = "visit-walletcheck-0002";
        String address = "0x1234567890abcdef";
        websiteVisitService.qualify(VisitSystem.WALLETCHECK,
            visit(visitId, 15, false, "/wallet/" + address + "?tab=overview", "/wallet/" + address + "/activity"),
            "127.0.0.1");
        websiteVisitService.heartbeat(VisitSystem.WALLETCHECK, visitId,
            new WebsiteVisitService.HeartbeatRequest(16, "/wallet/" + address + "?tab=history", false));

        var stored = websiteVisitRepository.findByVisitId(visitId).orElseThrow();
        assertEquals("/wallet/:address", stored.getEntryPath());
        assertEquals("/wallet/:address", stored.getLastPath());
    }

    @Test
    void preservesRecruitmentPathsForBackwardCompatibility() {
        String visitId = "visit-recruitment-0004";
        websiteVisitService.qualify(VisitSystem.RECRUITMENT,
            visit(visitId, 15, false, "/jobs?location=bangkok", "/about/team"), "127.0.0.1");
        websiteVisitService.heartbeat(VisitSystem.RECRUITMENT, visitId,
            new WebsiteVisitService.HeartbeatRequest(16, "/jobs/123?tab=description", false));

        var stored = websiteVisitRepository.findByVisitId(visitId).orElseThrow();
        assertEquals("/jobs?location=bangkok", stored.getEntryPath());
        assertEquals("/jobs/123?tab=description", stored.getLastPath());
    }

    @Test
    void storesOnlySafeShapesForWalletCheckPaths() {
        String visitId = "visit-walletcheck-0003";
        websiteVisitService.qualify(VisitSystem.WALLETCHECK,
            visit(visitId, 15, false,
                "/analyze?address=0xSecret#fragment",
                "/WALLET/0xSecret?tab=overview"),
            "127.0.0.1");

        var stored = websiteVisitRepository.findByVisitId(visitId).orElseThrow();
        assertEquals("/analyze", stored.getEntryPath());
        assertEquals("/wallet/:address", stored.getLastPath());

        websiteVisitService.heartbeat(VisitSystem.WALLETCHECK, visitId,
            new WebsiteVisitService.HeartbeatRequest(16, "/wallet%2F0xEncoded?tab=history", false));
        stored = websiteVisitRepository.findByVisitId(visitId).orElseThrow();
        assertEquals("/wallet/:address", stored.getLastPath());

        websiteVisitService.heartbeat(VisitSystem.WALLETCHECK, visitId,
            new WebsiteVisitService.HeartbeatRequest(17, "/unexpected/0xSecret?address=0xSecret", false));
        stored = websiteVisitRepository.findByVisitId(visitId).orElseThrow();
        assertEquals("/", stored.getLastPath());
    }

    @Test
    void filtersVisitsToCurrentBangkokDay() {
        websiteVisitService.qualify(visit("visit-000000000007", 15), "127.0.0.1");
        websiteVisitService.qualify(visit("visit-000000000008", 15), "127.0.0.1");
        var oldVisit = websiteVisitRepository.findByVisitId("visit-000000000007").orElseThrow();
        oldVisit.setQualifiedAt(Instant.parse("2020-01-01T00:00:00Z"));
        websiteVisitRepository.save(oldVisit);

        var filtered = websiteVisitService.list(0, 20, 0, true);

        assertEquals(1, filtered.getTotalElements());
        assertEquals("visit-000000000008", filtered.getContent().getFirst().getVisitId());
    }

    @Test
    void resolvesInitialLanguageFromAdminSettingBeforeVisitorCountry() {
        siteSettingsService.update("apple", "zh");
        assertEquals("zh", siteSettingsService.resolveInitialLanguage("US"));

        siteSettingsService.update("apple", "en");
        assertEquals("en", siteSettingsService.resolveInitialLanguage("CN"));

        siteSettingsService.update("apple", "auto");
        assertEquals("zh", siteSettingsService.resolveInitialLanguage("CN"));
        assertEquals("en", siteSettingsService.resolveInitialLanguage("TH"));
    }

    @Test
    void rejectsVisitsShorterThanFifteenSeconds() {
        assertThrows(IllegalArgumentException.class,
            () -> websiteVisitService.qualify(visit("visit-000000000003", 14), "127.0.0.1"));
    }

    @Test
    void deletesVisitById() {
        websiteVisitService.qualify(visit("visit-000000000004", 15), "127.0.0.1");
        long id = websiteVisitRepository.findByVisitId("visit-000000000004").orElseThrow().getId();

        websiteVisitService.delete(id);

        assertFalse(websiteVisitRepository.existsById(id));
    }

    @Test
    void deletesMultipleVisits() {
        websiteVisitService.qualify(visit("visit-000000000005", 15), "127.0.0.1");
        websiteVisitService.qualify(visit("visit-000000000006", 20), "127.0.0.1");
        List<Long> ids = websiteVisitRepository.findAll().stream().map(visit -> visit.getId()).toList();

        assertEquals(2, websiteVisitService.deleteAll(ids));
        assertEquals(0, websiteVisitRepository.count());
    }

    private WebsiteVisitService.VisitRequest visit(String id, int durationSeconds) {
        return visit(id, durationSeconds, false);
    }

    private WebsiteVisitService.VisitRequest visit(String id, int durationSeconds, boolean queriedAddress) {
        return visit(id, durationSeconds, queriedAddress, "/", "/jobs");
    }

    private WebsiteVisitService.VisitRequest visit(
        String id,
        int durationSeconds,
        boolean queriedAddress,
        String entryPath,
        String lastPath
    ) {
        return new WebsiteVisitService.VisitRequest(
            id, durationSeconds, entryPath, lastPath, "desktop", "Test PC", "Test OS",
            "Test Browser", "1920x1080", "zh-CN", "Asia/Shanghai", "test-agent", List.of(), queriedAddress
        );
    }
}
