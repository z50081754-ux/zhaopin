package com.xw.recruitment.job;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record JobRequest(
    @NotBlank String title,
    @NotBlank String category,
    String businessUnit,
    @NotBlank String requiredLocation,
    @NotBlank String workMode,
    String salaryRange,
    String internationalSalaryRange,
    String summary,
    List<String> responsibilities,
    List<String> requirements,
    List<String> bonus,
    @NotBlank String status,
    Integer recruitmentCount
) {}
