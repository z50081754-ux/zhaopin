package com.xw.recruitment.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xw.recruitment.application.ApplicationEntity;
import com.xw.recruitment.application.ApplicationService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.xw.recruitment.storage.FileStorage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminApplicationController {
    private final ApplicationService service;
    private final FileStorage storage;

    public AdminApplicationController(ApplicationService service, FileStorage storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping
    public ListResponse list(
        @RequestParam(defaultValue = "") String stage,
        @RequestParam(name = "q", defaultValue = "") String query,
        @RequestParam(defaultValue = "") String referrer,
        @RequestParam(defaultValue = "") String createdFrom,
        @RequestParam(defaultValue = "") String createdTo,
        @RequestParam(defaultValue = "") String operatingSystem,
        @RequestParam(defaultValue = "") String deviceModel,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<ApplicationEntity> result = service.search(stage, query, referrer, createdFrom, createdTo,
            operatingSystem, deviceModel, page, size);
        return new ListResponse(true, result.getContent().stream().map(AdminItem::from).toList(),
            result.getTotalElements(), result.getTotalPages());
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable long id) {
        service.delete(id);
        return Map.of("ok", true);
    }

    @GetMapping("/{id}")
    public AdminDetail detail(@PathVariable long id) {
        return AdminDetail.from(service.get(id));
    }

    @PatchMapping("/{id}/stage")
    public AdminItem updateStage(@PathVariable long id, @RequestBody StageRequest request) {
        return AdminItem.from(service.changeStage(id, request.stage()));
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable long id) {
        ApplicationEntity application = service.get(id);
        if (application.getResumeStorageKey() == null || application.getResumeStorageKey().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = storage.load(application.getResumeStorageKey());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(application.getResumeContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(application.getResumeOriginalFilename()).build().toString())
            .body(resource);
    }

    public record StageRequest(String stage) {}
    public record ListResponse(boolean ok, List<AdminItem> applications, long total, int pages) {}

    public record AdminItem(
        long id,
        @JsonProperty("application_no") String applicationNo,
        @JsonProperty("resume_name") String resumeName,
        String telegram,
        String gender,
        String age,
        @JsonProperty("birth_date") String birthDate,
        @JsonProperty("nationality_country") String nationalityCountry,
        @JsonProperty("job_title") String jobTitle,
        String referrer,
        String remarks,
        @JsonProperty("current_salary") String currentSalary,
        @JsonProperty("expected_salary") String expectedSalary,
        @JsonProperty("bc_experience") String bcExperience,
        @JsonProperty("employment_status") String employmentStatus,
        @JsonProperty("education_type") String educationType,
        String school,
        @JsonProperty("education_period") String educationPeriod,
        @JsonProperty("passport_status") String passportStatus,
        @JsonProperty("visa_status") String visaStatus,
        @JsonProperty("interview_time") String interviewTime,
        @JsonProperty("start_time") String startTime,
        @JsonProperty("current_country") String currentCountry,
        @JsonProperty("preferred_country") String preferredCountry,
        String stage,
        @JsonProperty("is_possible_duplicate") boolean possibleDuplicate,
        @JsonProperty("original_filename") String originalFilename,
        @JsonProperty("resume_size") long resumeSize,
        @JsonProperty("device_type") String deviceType,
        @JsonProperty("device_model") String deviceModel,
        @JsonProperty("operating_system") String operatingSystem,
        @JsonProperty("browser_name") String browserName,
        @JsonProperty("screen_resolution") String screenResolution,
        @JsonProperty("device_language") String deviceLanguage,
        @JsonProperty("device_timezone") String deviceTimezone,
        @JsonProperty("user_agent") String userAgent,
        @JsonProperty("created_at") Instant createdAt
    ) {
        static AdminItem from(ApplicationEntity a) {
            return new AdminItem(a.getId(), a.getApplicationNo(), a.getResumeName(), a.getTelegram(),
                a.getGender(), a.getAgeAtApplication(), a.getBirthDate(), a.getNationality(),
                a.getJobTitle(), a.getReferrer(), a.getRemarks(), a.getCurrentSalary(), a.getExpectedSalary(), a.getBcExperience(),
                a.getEmploymentStatus(), a.getEducationType(), a.getSchool(), a.getEducationPeriod(),
                a.getPassportStatus(), a.getVisaStatus(), a.getInterviewTime(), a.getStartTime(),
                a.getCurrentCountry(), a.getPreferredCountry(), a.getStage(), a.isPossibleDuplicate(),
                a.getResumeOriginalFilename(), a.getResumeSize(), a.getDeviceType(), a.getDeviceModel(),
                a.getOperatingSystem(), a.getBrowserName(), a.getScreenResolution(),
                a.getDeviceLanguage(), a.getDeviceTimezone(), a.getUserAgent(), a.getCreatedAt());
        }
    }

    public record AdminDetail(
        AdminItem summary,
        String gender,
        String age,
        @JsonProperty("birth_date") String birthDate,
        @JsonProperty("current_salary") String currentSalary,
        @JsonProperty("bc_experience") String bcExperience,
        @JsonProperty("employment_status") String employmentStatus,
        @JsonProperty("education_type") String educationType,
        String school,
        @JsonProperty("education_period") String educationPeriod,
        @JsonProperty("passport_status") String passportStatus,
        @JsonProperty("visa_status") String visaStatus,
        @JsonProperty("interview_time") String interviewTime,
        @JsonProperty("start_time") String startTime,
        @JsonProperty("device_type") String deviceType,
        @JsonProperty("device_model") String deviceModel,
        @JsonProperty("operating_system") String operatingSystem,
        @JsonProperty("browser_name") String browserName,
        @JsonProperty("screen_resolution") String screenResolution,
        @JsonProperty("device_language") String deviceLanguage,
        @JsonProperty("device_timezone") String deviceTimezone,
        @JsonProperty("user_agent") String userAgent
    ) {
        static AdminDetail from(ApplicationEntity a) {
            return new AdminDetail(AdminItem.from(a), a.getGender(), a.getAgeAtApplication(), a.getBirthDate(),
                a.getCurrentSalary(), a.getBcExperience(), a.getEmploymentStatus(), a.getEducationType(),
                a.getSchool(), a.getEducationPeriod(), a.getPassportStatus(), a.getVisaStatus(),
                a.getInterviewTime(), a.getStartTime(), a.getDeviceType(), a.getDeviceModel(),
                a.getOperatingSystem(), a.getBrowserName(), a.getScreenResolution(),
                a.getDeviceLanguage(), a.getDeviceTimezone(), a.getUserAgent());
        }
    }
}
