package com.xw.recruitment;

import com.xw.recruitment.visit.WebsiteVisitRepository;
import com.xw.recruitment.visit.WebsiteVisitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @BeforeEach
    void clearVisits() {
        websiteVisitRepository.deleteAll();
    }

    @Test
    void contextLoads() {}

    @Test
    void tracksVisitsAtTenSecondsAndFiltersByMinimumDuration() {
        WebsiteVisitService.VisitRequest tenSecondVisit = visit("visit-000000000001", 10);
        WebsiteVisitService.VisitRequest thirtySecondVisit = visit("visit-000000000002", 30);

        assertTrue(websiteVisitService.qualify(tenSecondVisit, "127.0.0.1").tracked());
        assertTrue(websiteVisitService.qualify(thirtySecondVisit, "127.0.0.1").tracked());
        assertEquals(2, websiteVisitRepository.count());

        var filtered = websiteVisitService.list(0, 20, 20);
        assertEquals(1, filtered.getTotalElements());
        assertEquals(30, filtered.getContent().getFirst().getDurationSeconds());
    }

    @Test
    void rejectsVisitsShorterThanTenSeconds() {
        assertThrows(IllegalArgumentException.class,
            () -> websiteVisitService.qualify(visit("visit-000000000003", 9), "127.0.0.1"));
    }

    private WebsiteVisitService.VisitRequest visit(String id, int durationSeconds) {
        return new WebsiteVisitService.VisitRequest(
            id, durationSeconds, "/", "/jobs", "desktop", "Test PC", "Test OS",
            "Test Browser", "1920x1080", "zh-CN", "Asia/Shanghai", "test-agent"
        );
    }
}
