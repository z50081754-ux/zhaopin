package com.xw.recruitment.job;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record JobResponse(
    long id,
    String slug,
    String title,
    String category,
    String businessUnit,
    String requiredLocation,
    String workMode,
    String salaryRange,
    String internationalSalaryRange,
    String summary,
    List<String> responsibilities,
    List<String> requirements,
    List<String> bonus,
    String status,
    int recruitmentCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static JobResponse from(JobEntity job) {
        return new JobResponse(job.getId(), job.getSlug(), job.getTitle(), job.getCategory(),
            job.getBusinessUnit(), job.getRequiredLocation(), job.getWorkMode(), job.getSalaryRange(),
            job.getInternationalSalaryRange(),
            job.getSummary(), lines(job.getResponsibilities()), lines(job.getRequirements()),
            lines(job.getBonus()), job.getStatus(), job.getRecruitmentCount(), job.getCreatedAt(), job.getUpdatedAt());
    }

    public static JobResponse from(JobEntity job, boolean internationalSalary) {
        JobResponse original = from(job);
        String selectedSalary = internationalSalary && job.getInternationalSalaryRange() != null
            && !job.getInternationalSalaryRange().isBlank()
            ? job.getInternationalSalaryRange() : job.getSalaryRange();
        return new JobResponse(original.id(), original.slug(), original.title(), original.category(),
            original.businessUnit(), original.requiredLocation(), original.workMode(), selectedSalary,
            null, original.summary(), original.responsibilities(), original.requirements(), original.bonus(),
            original.status(), original.recruitmentCount(), original.createdAt(), original.updatedAt());
    }

    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\R")).map(String::trim).filter(line -> !line.isBlank()).toList();
    }
}
