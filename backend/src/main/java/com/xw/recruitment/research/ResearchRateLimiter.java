package com.xw.recruitment.research;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResearchRateLimiter {
    private static final int DEFAULT_MAX_CLIENTS = 10_000;
    private static final int MAX_EXPIRED_EVICTIONS_PER_ACQUIRE = 64;

    private final Map<String, ArrayDeque<Instant>> windows =
        new LinkedHashMap<>(128, 0.75f, true);
    private final Clock clock;
    private final int limit;
    private final int maxClients;

    @Autowired
    public ResearchRateLimiter(Clock clock,
            @Value("${xw.research.rate-limit-per-minute:5}") int limit,
            @Value("${xw.research.rate-limit-max-clients:10000}") int maxClients) {
        if (limit < 1) throw new IllegalStateException("Research rate limit must be positive");
        if (maxClients < 1) {
            throw new IllegalStateException("Research rate limit client bound must be positive");
        }
        this.clock = clock;
        this.limit = limit;
        this.maxClients = maxClients;
    }

    ResearchRateLimiter(Clock clock, int limit) {
        this(clock, limit, DEFAULT_MAX_CLIENTS);
    }

    public synchronized void acquire(String ipHash) {
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(60);
        ArrayDeque<Instant> window = windows.get(ipHash);
        if (window == null) {
            evictExpiredWindows(cutoff);
            if (windows.size() >= maxClients) evictLeastRecentlyUsed();
            window = new ArrayDeque<>();
            windows.put(ipHash, window);
        }
        removeExpired(window, cutoff);
        if (window.size() >= limit) {
            throw ResearchApiException.tooManyRequests("RATE_LIMITED");
        }
        window.addLast(now);
    }

    private void evictExpiredWindows(Instant cutoff) {
        Iterator<Map.Entry<String, ArrayDeque<Instant>>> entries = windows.entrySet().iterator();
        int inspected = 0;
        while (entries.hasNext() && inspected < MAX_EXPIRED_EVICTIONS_PER_ACQUIRE) {
            ArrayDeque<Instant> candidate = entries.next().getValue();
            inspected++;
            removeExpired(candidate, cutoff);
            if (candidate.isEmpty()) entries.remove();
        }
    }

    private void evictLeastRecentlyUsed() {
        Iterator<String> clients = windows.keySet().iterator();
        if (clients.hasNext()) {
            clients.next();
            clients.remove();
        }
    }

    private void removeExpired(ArrayDeque<Instant> window, Instant cutoff) {
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.removeFirst();
        }
    }
}
