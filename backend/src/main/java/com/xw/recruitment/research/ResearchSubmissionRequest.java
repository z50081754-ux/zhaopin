package com.xw.recruitment.research;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record ResearchSubmissionRequest(
    @NotBlank String source,
    @Min(1) @Max(5) int rating,
    @NotEmpty Set<String> scenes,
    @NotBlank String concern,
    @Size(max = 500) String feedback,
    @NotBlank String walletNetwork,
    @NotBlank String walletAddress,
    @NotBlank String termsVersion,
    @AssertTrue boolean consent
) {}
