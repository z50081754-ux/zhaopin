package com.xw.recruitment.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:visit-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes"
})
class WebsiteVisitConcurrencyTest {
    @Autowired WebsiteVisitService service;
    @Autowired WebsiteVisitRepository repository;

    @BeforeEach
    void clearVisits() {
        repository.deleteAll();
    }

    @Test
    void concurrentHeartbeatsCannotRegressDurationOrQueryFlag() throws Exception {
        String visitId = "visit-concurrent-0001";
        service.qualify(VisitSystem.WALLETCHECK, visit(visitId), "127.0.0.1");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> high = executor.submit(() -> {
                ready.countDown();
                await(start);
                repository.mergeVisitState(
                    VisitSystem.WALLETCHECK.code(), visitId, 40, "/wallet/:address", Instant.now(), true);
            });
            Future<?> low = executor.submit(() -> {
                ready.countDown();
                await(start);
                repository.mergeVisitState(
                    VisitSystem.WALLETCHECK.code(), visitId, 16, "/", Instant.now(), false);
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            high.get(10, TimeUnit.SECONDS);
            low.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        WebsiteVisitEntity stored = repository.findByVisitId(visitId).orElseThrow();
        assertEquals(40, stored.getDurationSeconds());
        assertTrue(stored.isQueriedAddress());
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for concurrent start.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating concurrent updates.", exception);
        }
    }

    private WebsiteVisitService.VisitRequest visit(String visitId) {
        return new WebsiteVisitService.VisitRequest(
            visitId, 15, "/", "/", "desktop", "Test PC", "Test OS", "Test Browser",
            "1920x1080", "zh-CN", "Asia/Bangkok", "test-agent", List.of(), false
        );
    }
}
