package com.xw.recruitment.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ResearchRateLimiterTest {
    @Test
    void enforcesOneSharedLimitUnderConcurrentRequests() throws Exception {
        ResearchRateLimiter limiter = new ResearchRateLimiter(
            Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC), 5);
        ExecutorService executor = Executors.newFixedThreadPool(12);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int request = 0; request < 20; request++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        limiter.acquire("same-client");
                        return "ACCEPTED";
                    } catch (ResearchApiException exception) {
                        return exception.code();
                    }
                }));
            }
            start.countDown();

            List<String> outcomes = new ArrayList<>();
            for (Future<String> result : results) outcomes.add(result.get(5, TimeUnit.SECONDS));
            assertEquals(5, outcomes.stream().filter("ACCEPTED"::equals).count());
            assertEquals(15, outcomes.stream().filter("RATE_LIMITED"::equals).count());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void acceptsTheClientAgainAfterTheRateWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-28T00:00:00Z"));
        ResearchRateLimiter limiter = new ResearchRateLimiter(clock, 2);
        limiter.acquire("recovering-client");
        limiter.acquire("recovering-client");
        assertThrows(ResearchApiException.class, () -> limiter.acquire("recovering-client"));

        clock.advance(Duration.ofSeconds(61));

        limiter.acquire("recovering-client");
    }

    @Test
    void boundsTrackedClientWindowsDuringHighCardinalityTraffic() throws Exception {
        ResearchRateLimiter limiter = new ResearchRateLimiter(
            Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC), 2);

        for (int client = 0; client < 10_001; client++) {
            limiter.acquire("client-" + client);
        }

        Field windowsField = ResearchRateLimiter.class.getDeclaredField("windows");
        windowsField.setAccessible(true);
        Map<?, ?> windows = (Map<?, ?>) windowsField.get(limiter);
        assertTrue(windows.size() <= 10_000,
            () -> "Expected at most 10000 tracked windows but found " + windows.size());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
