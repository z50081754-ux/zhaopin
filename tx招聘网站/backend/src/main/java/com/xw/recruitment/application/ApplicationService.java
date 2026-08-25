package com.xw.recruitment.application;

import com.xw.recruitment.storage.FileStorage;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {
    private static final Instant MAX_SEARCH_TIME = Instant.parse("9999-12-31T23:59:59Z");
    private final ApplicationRepository repository;
    private final FileStorage storage;

    public ApplicationService(ApplicationRepository repository, FileStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Transactional
    public ApplicationEntity submit(ApplicationRequest request, String ipAddress) {
        String referrer = clean(request.referrer());
        if (referrer.isBlank()) throw new IllegalArgumentException("Referrer is required.");
        Instant now = Instant.now();
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(now);
        String applicationNo = "XW-" + date + "-" + UUID.randomUUID().toString()
            .substring(0, 8).toUpperCase(Locale.ROOT);
        FileStorage.StoredFile stored = request.resume() == null || request.resume().isEmpty()
            ? null : storage.save(request.resume(), applicationNo);

        ApplicationEntity entity = new ApplicationEntity();
        entity.setApplicationNo(applicationNo);
        entity.setResumeName(clean(request.resumeName()));
        entity.setTelegram(clean(request.telegram()));
        entity.setGender(clean(request.gender()));
        entity.setAgeAtApplication(clean(request.age()));
        entity.setBirthDate(clean(request.birthDate()));
        entity.setNationality(clean(request.nationality()));
        entity.setJobTitle(clean(request.job()));
        entity.setReferrer(referrer);
        entity.setRemarks(clean(request.remarks(), 2000));
        entity.setCurrentSalary(clean(request.currentSalary()));
        entity.setExpectedSalary(clean(request.expectedSalary()));
        entity.setBcExperience(clean(request.bcExperience()));
        entity.setEmploymentStatus(clean(request.employmentStatus()));
        entity.setEducationType(clean(request.educationType()));
        entity.setSchool(clean(request.school()));
        entity.setEducationPeriod(clean(request.educationPeriod()));
        entity.setPassportStatus(clean(request.passport()));
        entity.setVisaStatus(clean(request.visa()));
        entity.setInterviewTime(clean(request.interviewTime()));
        entity.setStartTime(clean(request.startTime()));
        entity.setCurrentCountry(clean(request.currentCountry()));
        entity.setPreferredCountry(clean(request.preferredCountry()));
        entity.setIpAddress(clean(ipAddress, 64));
        entity.setDeviceType(clean(request.deviceType()));
        entity.setDeviceModel(clean(request.deviceModel()));
        entity.setOperatingSystem(clean(request.operatingSystem()));
        entity.setBrowserName(clean(request.browserName()));
        entity.setScreenResolution(clean(request.screenResolution()));
        entity.setDeviceLanguage(clean(request.deviceLanguage()));
        entity.setDeviceTimezone(clean(request.deviceTimezone()));
        entity.setUserAgent(clean(request.userAgent()));
        entity.setStage("new");
        entity.setPossibleDuplicate(!entity.getTelegram().isBlank() && repository.existsByTelegramIgnoreCase(entity.getTelegram()));
        entity.setResumeStorageKey(stored == null ? "" : stored.storageKey());
        entity.setResumeOriginalFilename(stored == null ? "" : stored.filename());
        entity.setResumeContentType(stored == null ? "" : stored.contentType());
        entity.setResumeSize(stored == null ? 0 : stored.size());
        entity.setPrivacyPolicyVersion(Boolean.TRUE.equals(request.consent()) ? "2026-07" : "");
        entity.setConsentedAt(Boolean.TRUE.equals(request.consent()) ? now : null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return repository.save(entity);
    }

    public Page<ApplicationEntity> search(String stage, String query, String referrer, String createdFrom,
                                          String createdTo, String operatingSystem, String deviceModel,
                                          int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return repository.search(
            clean(stage),
            clean(query),
            clean(referrer),
            clean(operatingSystem),
            clean(deviceModel),
            startOfDay(createdFrom),
            nextDay(createdTo),
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @Transactional
    public void delete(long id) {
        ApplicationEntity entity = get(id);
        storage.delete(entity.getResumeStorageKey());
        repository.delete(entity);
    }

    @Transactional
    public int deleteAll(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids.stream().filter(id -> id != null && id > 0).distinct().limit(100).toList();
        List<ApplicationEntity> entities = repository.findAllById(safeIds);
        entities.forEach(entity -> storage.delete(entity.getResumeStorageKey()));
        repository.deleteAll(entities);
        return entities.size();
    }

    private Instant startOfDay(String value) {
        if (value == null || value.isBlank()) return Instant.EPOCH;
        return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant nextDay(String value) {
        if (value == null || value.isBlank()) return MAX_SEARCH_TIME;
        return LocalDate.parse(value).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public ApplicationEntity get(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Application not found."));
    }

    @Transactional
    public ApplicationEntity changeStage(long id, String stage) {
        if (!ApplicationStage.isAllowed(stage)) throw new IllegalArgumentException("Invalid stage.");
        ApplicationEntity entity = get(id);
        entity.setStage(stage);
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity);
    }

    private String clean(String value) {
        return clean(value, 1000);
    }

    private String clean(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
