package com.xw.recruitment.research;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResearchRateLimiter {
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int limit;

    public ResearchRateLimiter(Clock clock,
            @Value("${xw.research.rate-limit-per-minute:5}") int limit) {
        if (limit < 1) throw new IllegalStateException("Research rate limit must be positive");
        this.clock = clock;
        this.limit = limit;
    }

    public void acquire(String ipHash) {
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(60);
        removeExpiredWindows(ipHash, cutoff);
        windows.compute(ipHash, (ignored, existing) -> {
            ArrayDeque<Instant> window = existing == null ? new ArrayDeque<>() : existing;
            synchronized (window) {
                removeExpired(window, cutoff);
                if (window.size() >= limit) {
                    throw ResearchApiException.tooManyRequests("RATE_LIMITED");
                }
                window.addLast(now);
            }
            return window;
        });
    }

    private void removeExpiredWindows(String currentIpHash, Instant cutoff) {
        for (String key : windows.keySet()) {
            if (key.equals(currentIpHash)) continue;
            windows.computeIfPresent(key, (ignored, window) -> {
                synchronized (window) {
                    removeExpired(window, cutoff);
                    return window.isEmpty() ? null : window;
                }
            });
        }
    }

    private void removeExpired(ArrayDeque<Instant> window, Instant cutoff) {
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.removeFirst();
        }
    }
}
