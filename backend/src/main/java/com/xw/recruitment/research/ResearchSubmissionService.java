package com.xw.recruitment.research;

import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchSubmissionService {
    static final Set<String> SOURCES = Set.of("OPEN_CARD", "APP_DOWNLOAD", "FREE_CARD");
    static final Set<String> SCENES = Set.of("SUBSCRIPTIONS", "SHOPPING", "ATM", "TRAVEL", "GAMING", "ADS");
    static final Set<String> CONCERNS = Set.of("FEES", "SECURITY", "REGIONS", "SPEED");

    private static final long CAMPAIGN_ID = 1L;
    private static final String DISPLAY_NAME = "web3钱包产品调研";
    private static final String NETWORK = "TRC20";
    private static final DateTimeFormatter SUBMISSION_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final ResearchCampaignRepository campaigns;
    private final ResearchSubmissionRepository submissions;
    private final ResearchCryptoService crypto;
    private final ResearchRateLimiter rateLimiter;
    private final Validator validator;
    private final Clock clock;
    private final boolean enabled;
    private final TronAddressValidator addressValidator = new TronAddressValidator();

    public ResearchSubmissionService(ResearchCampaignRepository campaigns,
            ResearchSubmissionRepository submissions, ResearchCryptoService crypto,
            ResearchRateLimiter rateLimiter, Validator validator, Clock clock,
            @Value("${xw.research.enabled:false}") boolean enabled) {
        this.campaigns = campaigns;
        this.submissions = submissions;
        this.crypto = crypto;
        this.rateLimiter = rateLimiter;
        this.validator = validator;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Transactional(readOnly = true)
    public CampaignResult publicCampaign() {
        ResearchCampaignEntity campaign = campaigns.findById(CAMPAIGN_ID).orElse(null);
        String status = enabled && campaign != null && "ACTIVE".equals(campaign.getStatus())
            ? "ACTIVE" : "PAUSED";
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

    private void requireEnabled() {
        if (!enabled) {
            throw ResearchApiException.unavailable(
                "CAMPAIGN_PAUSED", "Research campaign is not accepting submissions");
        }
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

    private record NormalizedSubmission(String source, Set<String> scenes, String concern,
                                        String feedback, String walletAddress) {}
}
