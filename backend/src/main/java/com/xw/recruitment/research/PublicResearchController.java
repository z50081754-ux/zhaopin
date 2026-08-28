package com.xw.recruitment.research;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research")
public class PublicResearchController {
    private final ResearchSubmissionService service;
    private final ResearchClientIpResolver clientIpResolver;

    public PublicResearchController(ResearchSubmissionService service,
            ResearchClientIpResolver clientIpResolver) {
        this.service = service;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/campaign")
    public CampaignResponse campaign() {
        return CampaignResponse.from(service.publicCampaign());
    }

    @PostMapping(value = "/submissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitResponse submit(@Valid @RequestBody ResearchSubmissionRequest body,
            HttpServletRequest request) {
        return SubmitResponse.from(service.submit(body, clientIpResolver.clientIp(request),
            request.getHeader("User-Agent")));
    }

    public record CampaignResponse(String name, String status, String walletNetwork,
                                   String termsVersion) {
        static CampaignResponse from(ResearchSubmissionService.CampaignResult result) {
            return new CampaignResponse(result.name(), result.status(), result.walletNetwork(),
                result.termsVersion());
        }
    }

    public record SubmitResponse(String submissionNumber, String maskedWalletAddress,
                                 Instant submittedAt) {
        static SubmitResponse from(ResearchSubmissionService.SubmitResult result) {
            return new SubmitResponse(result.submissionNumber(), result.maskedWalletAddress(),
                result.submittedAt());
        }
    }
}
