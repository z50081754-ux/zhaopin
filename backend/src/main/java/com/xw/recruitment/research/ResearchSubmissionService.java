package com.xw.recruitment.research;

import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResearchSubmissionService {
    static final Set<String> SOURCES = Set.of("OPEN_CARD", "APP_DOWNLOAD", "FREE_CARD");
    static final Set<String> SCENES = Set.of("SUBSCRIPTIONS", "SHOPPING", "ATM", "TRAVEL", "GAMING", "ADS");
    static final Set<String> CONCERNS = Set.of("FEES", "SECURITY", "REGIONS", "SPEED");

    private static final long CAMPAIGN_ID = 1L;
    private static final String DISPLAY_NAME = "web3钱包产品调研";
    private static final String NETWORK = "TRC20";
    private static final Instant MAX_SEARCH_TIME = Instant.parse("9999-12-31T23:59:59Z");
    private static final DateTimeFormatter SUBMISSION_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ResearchCampaignRepository campaigns;
    private final ResearchSubmissionRepository submissions;
    private final ResearchCryptoService crypto;
    private final ResearchRateLimiter rateLimiter;
    private final Validator validator;
    private final EntityManager entityManager;
    private final Clock clock;
    private final boolean enabled;
    private final TronAddressValidator addressValidator = new TronAddressValidator();

    public ResearchSubmissionService(ResearchCampaignRepository campaigns,
            ResearchSubmissionRepository submissions, ResearchCryptoService crypto,
            ResearchRateLimiter rateLimiter, Validator validator, EntityManager entityManager,
            Clock clock,
            @Value("${xw.research.enabled:false}") boolean enabled) {
        this.campaigns = campaigns;
        this.submissions = submissions;
        this.crypto = crypto;
        this.rateLimiter = rateLimiter;
        this.validator = validator;
        this.entityManager = entityManager;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Transactional(readOnly = true)
    public CampaignResult publicCampaign() {
        ResearchCampaignEntity campaign = campaigns.findById(CAMPAIGN_ID).orElse(null);
        String status = effectiveCampaignStatus(campaign);
        String termsVersion = campaign == null ? "" : campaign.getTermsVersion();
        return new CampaignResult(DISPLAY_NAME, status, NETWORK, termsVersion);
    }

    @Transactional
    public SubmitResult submit(ResearchSubmissionRequest request, String ip, String userAgent) {
        requireEnabled();
        ResearchCampaignEntity campaign = campaigns.findById(CAMPAIGN_ID)
            .orElseThrow(() -> ResearchApiException.unavailable(
                "CAMPAIGN_PAUSED", "Research campaign is not accepting submissions"));
        if (!"ACTIVE".equals(campaign.getStatus())) {
            throw ResearchApiException.unavailable(
                "CAMPAIGN_PAUSED", "Research campaign is not accepting submissions");
        }
        if (request == null || !campaign.getTermsVersion().equals(request.termsVersion())) {
            throw ResearchApiException.conflict(
                "TERMS_VERSION_MISMATCH", "Research terms have changed");
        }

        NormalizedSubmission normalized = normalizeAndValidate(request);
        String ipHash = crypto.privacyHash(normalizeContext(ip));
        rateLimiter.acquire(ipHash);
        String walletHash = crypto.walletHash(normalized.walletAddress());
        if (submissions.existsByWalletHash(walletHash)) {
            throw duplicateWallet();
        }

        ResearchCryptoService.EncryptedWallet encrypted =
            crypto.encryptWallet(normalized.walletAddress());
        Instant now = clock.instant();
        String submissionNumber = generateSubmissionNumber(now);
        ResearchSubmissionEntity entity = new ResearchSubmissionEntity(
            submissionNumber, normalized.source(), request.rating(), normalized.concern(),
            normalized.feedback(), encrypted.ciphertext(), encrypted.nonce(), walletHash,
            ipHash, crypto.privacyHash(normalizeContext(userAgent)), campaign.getTermsVersion(),
            now, now, normalized.scenes());
        try {
            ResearchSubmissionEntity saved = submissions.saveAndFlush(entity);
            return new SubmitResult(saved.getSubmissionNumber(),
                maskWallet(normalized.walletAddress()), saved.getCreatedAt());
        } catch (DataIntegrityViolationException exception) {
            throw duplicateWallet();
        }
    }

    @Transactional(readOnly = true)
    public ResearchSummary researchSummary() {
        Double average = submissions.averageRating();
        return new ResearchSummary(
            submissions.count(),
            average == null ? 0.0 : average,
            integerDistribution(submissions.countByRating()),
            stringDistribution(submissions.countByScene()),
            stringDistribution(submissions.countByConcern()),
            stringDistribution(submissions.countBySource()));
    }

    @Transactional(readOnly = true)
    public ResearchListResponse searchResearch(ResearchFilters filters, int page, int size) {
        requireCryptoAvailable();
        ResearchFilters safeFilters = filters == null ? ResearchFilters.empty() : filters;
        Page<ResearchSubmissionEntity> result = search(safeFilters,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ResearchListResponse(result.getContent().stream()
            .map(this::toListItem)
            .toList(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ResearchDetail researchDetail(long id) {
        requireCryptoAvailable();
        return toDetail(requireSubmission(id));
    }

    @Transactional(readOnly = true)
    public ResearchDetail lookupResearchWallet(String walletAddress) {
        requireCryptoAvailable();
        String normalized = walletAddress == null ? "" : walletAddress.trim();
        if (!addressValidator.isValid(normalized)) {
            throw new IllegalArgumentException("Invalid wallet address");
        }
        ResearchSubmissionEntity entity = submissions.findByWalletHash(crypto.walletHash(normalized))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toDetail(entity);
    }

    @Transactional(readOnly = true)
    public String exportCsv(ResearchFilters filters) {
        requireCryptoAvailable();
        ResearchFilters safeFilters = filters == null ? ResearchFilters.empty() : filters;
        List<ResearchSubmissionEntity> entities = search(safeFilters,
            Pageable.unpaged(Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        StringBuilder csv = new StringBuilder("\uFEFF")
            .append("submission number,source,rating,scenes,concern,feedback,network,full wallet address,terms version,consent time,created time\r\n");
        for (ResearchSubmissionEntity entity : entities) {
            String walletAddress = decryptWallet(entity);
            csv.append(csvRow(List.of(
                entity.getSubmissionNumber(),
                entity.getSource(),
                Integer.toString(entity.getRating()),
                entity.getScenes().stream().sorted().collect(Collectors.joining("|")),
                entity.getConcern(),
                entity.getFeedback(),
                entity.getWalletNetwork(),
                walletAddress,
                entity.getTermsVersion(),
                entity.getConsentedAt().toString(),
                entity.getCreatedAt().toString())));
        }
        return csv.toString();
    }

    @Transactional
    public void deleteResearch(long id) {
        submissions.delete(requireSubmission(id));
    }

    @Transactional
    public int deleteResearchBatch(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Invalid submission ids");
        }
        List<Long> distinctIds = ids.stream().distinct().toList();
        if (distinctIds.size() > 100) {
            throw new IllegalArgumentException("Too many submission ids");
        }
        List<ResearchSubmissionEntity> entities = submissions.findAllById(distinctIds);
        submissions.deleteAll(entities);
        return entities.size();
    }

    @Transactional(readOnly = true)
    public AdminCampaign adminCampaign() {
        ResearchCampaignEntity campaign = campaigns.findById(CAMPAIGN_ID)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new AdminCampaign(campaign.getStatus(), effectiveCampaignStatus(campaign),
            enabled, crypto.available(), campaign.getTermsVersion(), campaign.getUpdatedAt());
    }

    @Transactional
    public AdminCampaign updateCampaign(String status) {
        if (status == null || !Set.of("ACTIVE", "PAUSED").contains(status)) {
            throw new IllegalArgumentException("Invalid campaign status");
        }
        if ("ACTIVE".equals(status) && !enabled) {
            throw ResearchApiException.conflict(
                "RESEARCH_INTAKE_DISABLED", "Research intake is disabled by configuration");
        }
        if ("ACTIVE".equals(status)) requireCryptoAvailable();
        int updated = entityManager.createQuery("""
            update ResearchCampaignEntity campaign
               set campaign.status = :status, campaign.updatedAt = :updatedAt
             where campaign.id = :id
            """)
            .setParameter("status", status)
            .setParameter("updatedAt", clock.instant())
            .setParameter("id", CAMPAIGN_ID)
            .executeUpdate();
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        entityManager.clear();
        return adminCampaign();
    }

    private Page<ResearchSubmissionEntity> search(ResearchFilters filters, Pageable pageable) {
        return submissions.search(
            clean(filters.number()),
            filters.rating(),
            clean(filters.concern()),
            clean(filters.source()),
            clean(filters.scene()),
            startOfDay(filters.from()),
            nextDay(filters.to()),
            pageable);
    }

    private ResearchSubmissionEntity requireSubmission(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid submission id");
        }
        return submissions.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ResearchListItem toListItem(ResearchSubmissionEntity entity) {
        String walletAddress = decryptWallet(entity);
        return new ResearchListItem(entity.getId(), entity.getSubmissionNumber(),
            entity.getSource(), entity.getRating(), sortedScenes(entity), entity.getConcern(),
            entity.getFeedback(), maskWallet(walletAddress), entity.getCreatedAt());
    }

    private ResearchDetail toDetail(ResearchSubmissionEntity entity) {
        String walletAddress = decryptWallet(entity);
        return new ResearchDetail(entity.getId(), entity.getSubmissionNumber(),
            entity.getSource(), entity.getRating(), sortedScenes(entity), entity.getConcern(),
            entity.getFeedback(), entity.getWalletNetwork(), walletAddress,
            entity.getTermsVersion(), entity.getConsentedAt(), entity.getCreatedAt());
    }

    private String decryptWallet(ResearchSubmissionEntity entity) {
        return crypto.decryptWallet(new ResearchCryptoService.EncryptedWallet(
            entity.getWalletCiphertext(), entity.getWalletNonce()));
    }

    private List<String> sortedScenes(ResearchSubmissionEntity entity) {
        return entity.getScenes().stream().sorted().toList();
    }

    private Map<Integer, Long> integerDistribution(List<Object[]> rows) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(((Number) row[0]).intValue(),
            ((Number) row[1]).longValue()));
        return result;
    }

    private Map<String, Long> stringDistribution(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put((String) row[0], ((Number) row[1]).longValue()));
        return result;
    }

    private Instant startOfDay(String value) {
        if (value == null || value.isBlank()) {
            return Instant.EPOCH;
        }
        return parseDate(value).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant nextDay(String value) {
        if (value == null || value.isBlank()) {
            return MAX_SEARCH_TIME;
        }
        return parseDate(value).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid date", exception);
        }
    }

    private String csvRow(List<String> values) {
        return values.stream().map(this::csvCell).collect(Collectors.joining(",")) + "\r\n";
    }

    private String csvCell(String value) {
        String safe = formulaSafe(value == null ? "" : value);
        if (safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0
                || safe.indexOf('\r') >= 0 || safe.indexOf('\n') >= 0) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private String formulaSafe(String value) {
        int firstContent = 0;
        while (firstContent < value.length() && Character.isWhitespace(value.charAt(firstContent))) {
            firstContent++;
        }
        if (firstContent < value.length()
                && "=+-@".indexOf(value.charAt(firstContent)) >= 0) {
            return "'" + value;
        }
        if (!value.isEmpty() && (value.charAt(0) == '\t' || value.charAt(0) == '\r')) {
            return "'" + value;
        }
        return value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void requireEnabled() {
        if (!enabled) {
            throw ResearchApiException.unavailable(
                "CAMPAIGN_PAUSED", "Research campaign is not accepting submissions");
        }
    }

    private void requireCryptoAvailable() {
        if (!crypto.available()) {
            throw ResearchApiException.unavailable(
                "RESEARCH_DATA_UNAVAILABLE", "Research encrypted data is unavailable");
        }
    }

    private String effectiveCampaignStatus(ResearchCampaignEntity campaign) {
        if (!enabled) return "DISABLED";
        if (!crypto.available() || campaign == null) return "UNAVAILABLE";
        if ("ACTIVE".equals(campaign.getStatus()) || "PAUSED".equals(campaign.getStatus())) {
            return campaign.getStatus();
        }
        return "UNAVAILABLE";
    }

    private NormalizedSubmission normalizeAndValidate(ResearchSubmissionRequest request) {
        if (!validator.validate(request).isEmpty()) {
            throw validationFailed();
        }
        String source = request.source().trim();
        String concern = request.concern().trim();
        String network = request.walletNetwork().trim();
        String walletAddress = request.walletAddress().trim();
        if (request.scenes().stream().anyMatch(scene -> scene == null)) {
            throw validationFailed();
        }
        Set<String> scenes = request.scenes().stream()
            .map(String::trim)
            .collect(Collectors.toUnmodifiableSet());
        if (!SOURCES.contains(source) || !SCENES.containsAll(scenes)
                || !CONCERNS.contains(concern) || !NETWORK.equals(network)) {
            throw validationFailed();
        }
        if (!addressValidator.isValid(walletAddress)) {
            throw ResearchApiException.badRequest(
                "INVALID_TRC20_ADDRESS", "A valid TRC20 wallet address is required");
        }
        String feedback = request.feedback() == null ? "" : request.feedback().trim();
        return new NormalizedSubmission(source, scenes, concern, feedback, walletAddress);
    }

    private ResearchApiException validationFailed() {
        return ResearchApiException.badRequest(
            "VALIDATION_FAILED", "Research submission validation failed");
    }

    private ResearchApiException duplicateWallet() {
        return ResearchApiException.conflict(
            "DUPLICATE_WALLET", "This wallet address has already been submitted");
    }

    private String generateSubmissionNumber(Instant submittedAt) {
        String suffix = UUID.randomUUID().toString().replace("-", "")
            .substring(0, 8).toUpperCase(Locale.ROOT);
        return "SP-" + SUBMISSION_DATE.format(submittedAt) + "-" + suffix;
    }

    private String maskWallet(String walletAddress) {
        return walletAddress.substring(0, 6) + "••••••"
            + walletAddress.substring(walletAddress.length() - 5);
    }

    private String normalizeContext(String value) {
        return value == null ? "" : value.trim();
    }

    public record CampaignResult(String name, String status, String walletNetwork,
                                 String termsVersion) {}

    public record SubmitResult(String submissionNumber, String maskedWalletAddress,
                               Instant submittedAt) {}

    public record ResearchFilters(String number, int rating, String concern, String source,
                                  String scene, String from, String to) {
        public static ResearchFilters empty() {
            return new ResearchFilters("", 0, "", "", "", "", "");
        }
    }

    public record ResearchSummary(
        long total,
        double averageRating,
        Map<Integer, Long> ratingDistribution,
        Map<String, Long> sceneDistribution,
        Map<String, Long> concernDistribution,
        Map<String, Long> sourceDistribution
    ) {}

    public record ResearchListResponse(List<ResearchListItem> submissions, long total, int pages) {}

    public record ResearchListItem(
        long id,
        String submissionNumber,
        String source,
        int rating,
        List<String> scenes,
        String concern,
        String feedback,
        String maskedWalletAddress,
        Instant createdAt
    ) {}

    public record ResearchDetail(
        long id,
        String submissionNumber,
        String source,
        int rating,
        List<String> scenes,
        String concern,
        String feedback,
        String walletNetwork,
        String walletAddress,
        String termsVersion,
        Instant consentedAt,
        Instant createdAt
    ) {}

    public record AdminCampaign(
        String status,
        String effectiveStatus,
        boolean intakeEnabled,
        boolean dataAvailable,
        String termsVersion,
        Instant updatedAt
    ) {}

    private record NormalizedSubmission(String source, Set<String> scenes, String concern,
                                        String feedback, String walletAddress) {}
}
