package com.xw.recruitment.application;

import org.springframework.web.multipart.MultipartFile;

public record ApplicationRequest(
    String resumeName,
    String telegram,
    String gender,
    String age,
    String birthDate,
    String nationality,
    String job,
    String referrer,
    String currentSalary,
    String expectedSalary,
    String bcExperience,
    String employmentStatus,
    String educationType,
    String school,
    String educationPeriod,
    String passport,
    String visa,
    String interviewTime,
    String startTime,
    String currentCountry,
    String preferredCountry,
    String deviceType,
    String deviceModel,
    String operatingSystem,
    String browserName,
    String screenResolution,
    String deviceLanguage,
    String deviceTimezone,
    String userAgent,
    Boolean consent,
    MultipartFile resume
) {}
