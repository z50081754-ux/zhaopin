package com.xw.recruitment.visit;

import com.xw.recruitment.research.ResearchSubmissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class WebsiteVisitService {
    private final WebsiteVisitRepository repository;
    private final ResearchSubmissionRepository researchSubmissions;

    public WebsiteVisitService(
        WebsiteVisitRepository repository,
        ResearchSubmissionRepository researchSubmissions
    ) {
        this.repository = repository;
        this.researchSubmissions = researchSubmissions;
    }

    @Transactional
    public synchronized QualifyResult qualify(VisitRequest request, String ipAddress) {
        return qualify(VisitSystem.RECRUITMENT, request, ipAddress, "UNKNOWN");
    }

    @Transactional
    public synchronized QualifyResult qualify(VisitSystem system, VisitRequest request, String ipAddress) {
        return qualify(system, request, ipAddress, "UNKNOWN");
    }

    @Transactional
    public synchronized QualifyResult qualify(
        VisitSystem system,
        VisitRequest request,
        String ipAddress,
        String visitorCountry
    ) {
        requireSystem(system);
        validateVisitId(request.visitId());
        int duration = boundedDuration(request.durationSeconds());
        if (duration < system.qualificationSeconds()) {
            throw new IllegalArgumentException(
                "Visit must be at least " + system.qualificationSeconds() + " seconds.");
        }
        Instant now = Instant.now();
        boolean submittedResearch = system == VisitSystem.RESEARCH
            && researchSubmissions.existsByVisitId(request.visitId());
        WebsiteVisitEntity visit = repository.findByVisitId(request.visitId()).orElse(null);
        if (visit != null && !system.code().equals(visit.getSystemCode())) {
            throw new IllegalArgumentException("Visit system does not match.");
        }
        if (visit == null) {
            visit = new WebsiteVisitEntity();
        }
        if (visit.getVisitId() == null) {
            visit.setVisitId(request.visitId());
            visit.setSystemCode(system.code());
            visit.setStartedAt(now.minusSeconds(duration));
            visit.setQualifiedAt(now);
            visit.setIpAddress(clean(ipAddress, 64));
            visit.setVisitorCountry(clean(visitorCountry, 16));
            visit.setEntryPath(normalizePath(system, request.entryPath()));
            visit.setDeviceType(clean(request.deviceType(), 80));
            visit.setDeviceModel(clean(request.deviceModel(), 200));
            visit.setOperatingSystem(clean(request.operatingSystem(), 200));
            visit.setBrowserName(clean(request.browserName(), 200));
            visit.setScreenResolution(clean(request.screenResolution(), 100));
            visit.setDeviceLanguage(clean(request.deviceLanguage(), 80));
            visit.setDeviceTimezone(clean(request.deviceTimezone(), 120));
            visit.setUserAgent(clean(request.userAgent(), 1000));
            visit.setDetectedWallets(cleanWallets(request.detectedWallets()));
            visit.setDurationSeconds(duration);
            visit.setLastPath(normalizePath(system, request.lastPath()));
            visit.setLastSeenAt(now);
            visit.setQueriedAddress(request.queriedAddress());
            visit.setSubmittedResearch(submittedResearch);
            repository.save(visit);
        } else {
            repository.mergeVisitState(
                system.code(), request.visitId(), duration, normalizePath(system, request.lastPath()), now,
                request.queriedAddress());
            if (submittedResearch) repository.markResearchSubmitted(request.visitId());
        }
        return new QualifyResult(true, false);
    }

    @Transactional
    public WebsiteVisitEntity heartbeat(String visitId, HeartbeatRequest request) {
        return heartbeat(VisitSystem.RECRUITMENT, visitId, request);
    }

    @Transactional
    public WebsiteVisitEntity heartbeat(VisitSystem system, String visitId, HeartbeatRequest request) {
        requireSystem(system);
        validateVisitId(visitId);
        WebsiteVisitEntity visit = repository.findByVisitId(visitId)
            .orElseThrow(() -> new IllegalArgumentException("Visit not found."));
        if (!system.code().equals(visit.getSystemCode())) throw new IllegalArgumentException("Visit system does not match.");
        int updated = repository.mergeVisitState(
            system.code(), visitId, boundedDuration(request.durationSeconds()), normalizePath(system, request.lastPath()),
            Instant.now(), request.queriedAddress());
        if (updated != 1) throw new IllegalArgumentException("Visit not found.");
        return repository.findByVisitId(visitId)
            .orElseThrow(() -> new IllegalArgumentException("Visit not found."));
    }

    public Page<WebsiteVisitEntity> list(int page, int size, int minDurationSeconds, boolean today) {
        return list(VisitSystem.RECRUITMENT, page, size, minDurationSeconds, today);
    }

    public Page<WebsiteVisitEntity> list(VisitSystem system, int page, int size, int minDurationSeconds, boolean today) {
        requireSystem(system);
        int safeDuration = Math.min(Math.max(minDurationSeconds, 0), 86400);
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100));
        if (!today) {
            return repository.findAllBySystemCodeAndDurationSecondsGreaterThanEqualOrderByQualifiedAtDesc(
                system.code(), safeDuration, pageable
            );
        }
        Instant bangkokTodayStart = LocalDate.now(ZoneId.of("Asia/Bangkok"))
            .atStartOfDay(ZoneId.of("Asia/Bangkok"))
            .toInstant();
        return repository.findAllBySystemCodeAndDurationSecondsGreaterThanEqualAndQualifiedAtGreaterThanEqualOrderByQualifiedAtDesc(
            system.code(), safeDuration, bangkokTodayStart, pageable
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

    private void requireSystem(VisitSystem system) {
        if (system == null) throw new IllegalArgumentException("Invalid visit system.");
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

    private String normalizePath(VisitSystem system, String value) {
        return switch (system) {
            case WALLETCHECK -> normalizeWalletCheckPath(value);
            case RESEARCH -> normalizeResearchPath(value);
            case RECRUITMENT -> normalizeRecruitmentPath(value);
        };
    }

    private String normalizeRecruitmentPath(String value) {
        String path = clean(value, 500);
        if (path.startsWith("/wallet/")) {
            String segment = path.substring("/wallet/".length());
            if (!segment.isEmpty() && segment.charAt(0) != '/' && segment.charAt(0) != '?' && segment.charAt(0) != '#') {
                return "/wallet/:address";
            }
        }
        return path;
    }

    private String normalizeWalletCheckPath(String value) {
        String path = clean(value, 500);
        for (int attempt = 0; attempt < 3; attempt++) {
            path = stripQueryAndFragment(path);
            try {
                String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
                if (decoded.equals(path)) break;
                path = decoded;
            } catch (IllegalArgumentException exception) {
                return "/";
            }
        }
        path = stripQueryAndFragment(path);
        if (path.regionMatches(true, 0, "/wallet/", 0, "/wallet/".length())) {
            String segment = path.substring("/wallet/".length());
            if (!segment.isEmpty() && segment.charAt(0) != '/') return "/wallet/:address";
        }
        if (path.equals("/") || path.equals("/analyze")) return path;
        return "/";
    }

    private String normalizeResearchPath(String value) {
        String path = stripQueryAndFragment(clean(value, 500));
        return path.equals("/") ? path : "/";
    }

    private String stripQueryAndFragment(String value) {
        int query = value.indexOf('?');
        int fragment = value.indexOf('#');
        int end = value.length();
        if (query >= 0) end = Math.min(end, query);
        if (fragment >= 0) end = Math.min(end, fragment);
        return value.substring(0, end);
    }

    public record VisitRequest(
        String visitId, int durationSeconds, String entryPath, String lastPath,
        String deviceType, String deviceModel, String operatingSystem, String browserName,
        String screenResolution, String deviceLanguage, String deviceTimezone, String userAgent,
        List<String> detectedWallets, boolean queriedAddress
    ) {
        public VisitRequest(
            String visitId, int durationSeconds, String entryPath, String lastPath,
            String deviceType, String deviceModel, String operatingSystem, String browserName,
            String screenResolution, String deviceLanguage, String deviceTimezone, String userAgent,
            List<String> detectedWallets
        ) {
            this(visitId, durationSeconds, entryPath, lastPath, deviceType, deviceModel, operatingSystem,
                browserName, screenResolution, deviceLanguage, deviceTimezone, userAgent, detectedWallets, false);
        }

        public VisitRequest(
            String visitId, int durationSeconds, String entryPath, String lastPath,
            String deviceType, String deviceModel, String operatingSystem, String browserName,
            String screenResolution, String deviceLanguage, String deviceTimezone, String userAgent
        ) {
            this(visitId, durationSeconds, entryPath, lastPath, deviceType, deviceModel, operatingSystem,
                browserName, screenResolution, deviceLanguage, deviceTimezone, userAgent, List.of(), false);
        }
    }
    public record HeartbeatRequest(int durationSeconds, String lastPath, boolean queriedAddress) {
        public HeartbeatRequest(int durationSeconds, String lastPath) {
            this(durationSeconds, lastPath, false);
        }
    }
    public record QualifyResult(boolean tracked, boolean duplicate) {}
}
