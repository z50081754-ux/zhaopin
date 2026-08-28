package com.xw.recruitment.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xw.recruitment.visit.VisitSystem;
import com.xw.recruitment.visit.WebsiteVisitEntity;
import com.xw.recruitment.visit.WebsiteVisitService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/visits")
public class AdminWebsiteVisitController {
    private final WebsiteVisitService service;

    public AdminWebsiteVisitController(WebsiteVisitService service) { this.service = service; }

    @GetMapping
    public ListResponse list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "0") int minDurationSeconds,
        @RequestParam(defaultValue = "86400") int maxDurationSeconds,
        @RequestParam(defaultValue = "false") boolean today,
        @RequestParam(defaultValue = "all") String submittedResearch,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "recruitment") String systemCode
    ) {
        Page<WebsiteVisitEntity> result = service.list(
            VisitSystem.fromCode(systemCode), page, size, minDurationSeconds,
            maxDurationSeconds, today, submittedResearch, from, to);
        return new ListResponse(result.getContent().stream().map(VisitItem::from).toList(),
            result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/summary")
    public VisitSummary summary(@RequestParam(defaultValue = "research") String systemCode) {
        return service.summary(VisitSystem.fromCode(systemCode),
            LocalDate.now(ZoneId.of("Asia/Bangkok")));
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable long id) {
        service.delete(id);
        return Map.of("ok", true);
    }

    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatch(@RequestBody BatchDeleteRequest request) {
        return Map.of("ok", true, "deleted", service.deleteAll(request.ids()));
    }

    public record BatchDeleteRequest(List<Long> ids) {}

    public record ListResponse(List<VisitItem> visits, long total, int pages) {}
    public record VisitSummary(
        long todayEffective,
        double averageDurationSeconds,
        int maxDurationSeconds,
        long submittedCount,
        double conversionRate
    ) {}

    public record VisitItem(
        long id,
        @JsonProperty("visit_id") String visitId,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("qualified_at") Instant qualifiedAt,
        @JsonProperty("last_seen_at") Instant lastSeenAt,
        @JsonProperty("duration_seconds") int durationSeconds,
        @JsonProperty("ip_address") String ipAddress,
        @JsonProperty("entry_path") String entryPath,
        @JsonProperty("last_path") String lastPath,
        @JsonProperty("device_type") String deviceType,
        @JsonProperty("device_model") String deviceModel,
        @JsonProperty("operating_system") String operatingSystem,
        @JsonProperty("browser_name") String browserName,
        @JsonProperty("screen_resolution") String screenResolution,
        @JsonProperty("device_language") String deviceLanguage,
        @JsonProperty("device_timezone") String deviceTimezone,
        @JsonProperty("user_agent") String userAgent,
        @JsonProperty("detected_wallets") String detectedWallets,
        @JsonProperty("system_code") String systemCode,
        @JsonProperty("queried_address") boolean queriedAddress,
        @JsonProperty("submitted_research") boolean submittedResearch,
        @JsonProperty("visitor_country") String visitorCountry
    ) {
        static VisitItem from(WebsiteVisitEntity visit) {
            return new VisitItem(visit.getId(), visit.getVisitId(), visit.getStartedAt(), visit.getQualifiedAt(),
                visit.getLastSeenAt(), visit.getDurationSeconds(), visit.getIpAddress(), visit.getEntryPath(), visit.getLastPath(),
                visit.getDeviceType(), visit.getDeviceModel(), visit.getOperatingSystem(), visit.getBrowserName(),
                visit.getScreenResolution(), visit.getDeviceLanguage(), visit.getDeviceTimezone(), visit.getUserAgent(),
                visit.getDetectedWallets(), visit.getSystemCode(), visit.isQueriedAddress(), visit.isSubmittedResearch(),
                visit.getVisitorCountry());
        }
    }
}
