package com.xw.recruitment.job;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JobService {
    private static final Pattern SALARY_RANGE = Pattern.compile(
        "(?i)^\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([k]?)\\s*(?:-|–|—|~|至)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*([k]?)\\s*(?:USDT)?\\s*(?:/月|每月|月)?\\s*$"
    );
    private static final Set<String> CATEGORIES = Set.of("职能岗位", "技术岗位");
    private static final Set<String> WORK_MODES = Set.of("居家", "远程");
    private static final Set<String> STATUSES = Set.of("draft", "open", "paused", "closed");
    private final JobRepository repository;

    public JobService(JobRepository repository) {
        this.repository = repository;
    }

    public List<JobResponse> publicJobs(boolean internationalSalary) {
        return repository.findAllByStatusOrderByUpdatedAtDesc("open").stream()
            .map(job -> JobResponse.from(job, internationalSalary)).toList();
    }

    public JobResponse publicJob(String slug, boolean internationalSalary) {
        return repository.findBySlugAndStatus(slug, "open").map(job -> JobResponse.from(job, internationalSalary))
            .orElseThrow(() -> new IllegalArgumentException("Job not found."));
    }

    public List<JobResponse> adminJobs() {
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(JobResponse::from).toList();
    }

    @Transactional
    public JobResponse create(JobRequest request) {
        validate(request);
        Instant now = Instant.now();
        JobEntity entity = new JobEntity();
        entity.setSlug(uniqueSlug(request.title()));
        apply(entity, request);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return JobResponse.from(repository.save(entity));
    }

    @Transactional
    public JobResponse update(long id, JobRequest request) {
        validate(request);
        JobEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Job not found."));
        apply(entity, request);
        entity.setUpdatedAt(Instant.now());
        return JobResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(long id) {
        JobEntity entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Job not found."));
        repository.delete(entity);
    }

    private void apply(JobEntity entity, JobRequest request) {
        entity.setTitle(clean(request.title(), 180));
        entity.setCategory(request.category());
        entity.setBusinessUnit(clean(request.businessUnit(), 120));
        entity.setRequiredLocation(clean(request.requiredLocation(), 160));
        entity.setWorkMode(request.workMode());
        String southeastAsiaSalary = normalizeSalaryRange(request.salaryRange());
        entity.setSalaryRange(southeastAsiaSalary);
        entity.setInternationalSalaryRange(internationalSalaryRange(southeastAsiaSalary));
        entity.setSummary(clean(request.summary(), 1000));
        entity.setResponsibilities(join(request.responsibilities()));
        entity.setRequirements(join(request.requirements()));
        entity.setBonus(join(request.bonus()));
        entity.setStatus(request.status());
        entity.setRecruitmentCount(request.recruitmentCount() == null ? 1 : Math.max(1, request.recruitmentCount()));
    }

    private void validate(JobRequest request) {
        if (!CATEGORIES.contains(request.category())) throw new IllegalArgumentException("Invalid job category.");
        if (!WORK_MODES.contains(request.workMode())) throw new IllegalArgumentException("Invalid work mode.");
        if (!STATUSES.contains(request.status())) throw new IllegalArgumentException("Invalid job status.");
    }

    private String uniqueSlug(String title) {
        String base = Normalizer.normalize(title, Normalizer.Form.NFKD)
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "job";
        String slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        return repository.existsBySlug(slug) ? slug + "-" + UUID.randomUUID().toString().substring(0, 4) : slug;
    }

    private String join(List<String> values) {
        if (values == null) return "";
        return values.stream().map(value -> clean(value, 1000)).filter(value -> !value.isBlank())
            .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    private String clean(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private String normalizeSalaryRange(String value) {
        String cleaned = clean(value, 120).replace(",", "");
        if (cleaned.isBlank()) return "";
        cleaned = cleaned.replaceAll("(?i)\\s*USDT\\s*(?:/月|每月|月)?\\s*$", "").trim();
        cleaned = cleaned.replaceAll("\\s*/\\s*月\\s*$", "").trim();
        Matcher matcher = SALARY_RANGE.matcher(cleaned);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("薪资范围请填写为数字区间，例如 2000-3000。");
        }
        return formatAmount(matcher.group(1), matcher.group(2)) + "–"
            + formatAmount(matcher.group(3), matcher.group(4)) + " USDT/月";
    }

    private String internationalSalaryRange(String southeastAsiaSalary) {
        if (southeastAsiaSalary.isBlank()) return "";
        Matcher matcher = SALARY_RANGE.matcher(southeastAsiaSalary);
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid stored salary range.");
        return multiplyAmount(matcher.group(1), matcher.group(2)) + "–"
            + multiplyAmount(matcher.group(3), matcher.group(4)) + " USDT/月";
    }

    private String formatAmount(String number, String suffix) {
        return formatDecimal(new BigDecimal(number)) + suffix.toUpperCase(Locale.ROOT);
    }

    private String multiplyAmount(String number, String suffix) {
        return formatDecimal(new BigDecimal(number).multiply(new BigDecimal("1.5")))
            + suffix.toUpperCase(Locale.ROOT);
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
