package com.xw.recruitment.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class WebsiteVisitService {
    private final WebsiteVisitRepository repository;

    public WebsiteVisitService(WebsiteVisitRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public synchronized QualifyResult qualify(VisitRequest request, String ipAddress) {
        validateVisitId(request.visitId());
        int duration = boundedDuration(request.durationSeconds());
        if (duration < 15) throw new IllegalArgumentException("Visit must be at least 15 seconds.");
        Instant now = Instant.now();
        WebsiteVisitEntity visit = repository.findByVisitId(request.visitId()).orElse(null);
        if (visit == null) {
            visit = new WebsiteVisitEntity();
        }
        if (visit.getVisitId() == null) {
            visit.setVisitId(request.visitId());
            visit.setStartedAt(now.minusSeconds(duration));
            visit.setQualifiedAt(now);
            visit.setIpAddress(clean(ipAddress, 64));
            visit.setEntryPath(clean(request.entryPath(), 500));
            visit.setDeviceType(clean(request.deviceType(), 80));
            visit.setDeviceModel(clean(request.deviceModel(), 200));
            visit.setOperatingSystem(clean(request.operatingSystem(), 200));
            visit.setBrowserName(clean(request.browserName(), 200));
            visit.setScreenResolution(clean(request.screenResolution(), 100));
            visit.setDeviceLanguage(clean(request.deviceLanguage(), 80));
            visit.setDeviceTimezone(clean(request.deviceTimezone(), 120));
            visit.setUserAgent(clean(request.userAgent(), 1000));
            visit.setDetectedWallets(cleanWallets(request.detectedWallets()));
        }
        visit.setDurationSeconds(Math.max(visit.getDurationSeconds(), duration));
        visit.setLastPath(clean(request.lastPath(), 500));
        visit.setLastSeenAt(now);
        repository.save(visit);
        return new QualifyResult(true, false);
    }

    @Transactional
    public WebsiteVisitEntity heartbeat(String visitId, HeartbeatRequest request) {
        validateVisitId(visitId);
        WebsiteVisitEntity visit = repository.findByVisitId(visitId)
            .orElseThrow(() -> new IllegalArgumentException("Visit not found."));
        visit.setDurationSeconds(Math.max(visit.getDurationSeconds(), boundedDuration(request.durationSeconds())));
        visit.setLastPath(clean(request.lastPath(), 500));
        visit.setLastSeenAt(Instant.now());
        return repository.save(visit);
    }

    public Page<WebsiteVisitEntity> list(int page, int size, int minDurationSeconds) {
        return repository.findAllByDurationSecondsGreaterThanEqualOrderByQualifiedAtDesc(
            Math.min(Math.max(minDurationSeconds, 0), 86400),
            PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100))
        );
    }

    @Transactional
    public void delete(long id) {
        if (!repository.existsById(id)) throw new IllegalArgumentException("Visit not found.");
        repository.deleteById(id);
    }

    @Transactional
    public int deleteAll(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids.stream().filter(id -> id != null && id > 0).distinct().limit(100).toList();
        List<WebsiteVisitEntity> visits = repository.findAllById(safeIds);
        repository.deleteAll(visits);
        return visits.size();
    }

    private void validateVisitId(String value) {
        if (value == null || !value.matches("[A-Za-z0-9-]{16,64}")) throw new IllegalArgumentException("Invalid visit id.");
    }

    private int boundedDuration(int value) { return Math.min(Math.max(value, 0), 86400); }
    private String cleanWallets(List<String> wallets) {
        if (wallets == null || wallets.isEmpty()) return "";
        List<String> allowed = List.of("Bitpie", "Trust Wallet", "Uniswap Wallet", "Phantom", "Coin98", "Solflare", "Bitget Wallet", "MyTonWallet", "Tonhub", "Tonkeeper", "TokenPocket", "TronLink", "MetaMask", "OKX Wallet", "Exodus", "Ronin Wallet", "imToken");
        return wallets.stream()
            .filter(allowed::contains)
            .distinct()
            .limit(allowed.size())
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    private String clean(String value, int max) {
        if (value == null) return "";
        String clean = value.trim();
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    public record VisitRequest(
        String visitId, int durationSeconds, String entryPath, String lastPath,
        String deviceType, String deviceModel, String operatingSystem, String browserName,
        String screenResolution, String deviceLanguage, String deviceTimezone, String userAgent,
        List<String> detectedWallets
    ) {
        public VisitRequest(
            String visitId, int durationSeconds, String entryPath, String lastPath,
            String deviceType, String deviceModel, String operatingSystem, String browserName,
            String screenResolution, String deviceLanguage, String deviceTimezone, String userAgent
        ) {
            this(visitId, durationSeconds, entryPath, lastPath, deviceType, deviceModel, operatingSystem,
                browserName, screenResolution, deviceLanguage, deviceTimezone, userAgent, List.of());
        }
    }
    public record HeartbeatRequest(int durationSeconds, String lastPath) {}
    public record QualifyResult(boolean tracked, boolean duplicate) {}
}
