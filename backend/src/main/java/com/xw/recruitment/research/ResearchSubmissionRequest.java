package com.xw.recruitment.research;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
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
    @AssertTrue boolean consent,
    @Pattern(regexp = "[A-Za-z0-9-]{16,64}") String visitId
) {
    public ResearchSubmissionRequest(
        String source, int rating, Set<String> scenes, String concern, String feedback,
        String walletNetwork, String walletAddress, String termsVersion, boolean consent
    ) {
        this(source, rating, scenes, concern, feedback, walletNetwork, walletAddress, termsVersion, consent, null);
    }
}
