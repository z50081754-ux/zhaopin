package com.xw.recruitment.application;

import com.xw.recruitment.config.VisitorRegionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class PublicApplicationController {
    private final ApplicationService service;
    private final VisitorRegionResolver regionResolver;

    public PublicApplicationController(ApplicationService service, VisitorRegionResolver regionResolver) {
        this.service = service;
        this.regionResolver = regionResolver;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitResponse submit(@Valid @ModelAttribute ApplicationRequest request, HttpServletRequest httpRequest) {
        ApplicationEntity saved = service.submit(request, regionResolver.ipAddress(httpRequest));
        return new SubmitResponse(true, saved.getApplicationNo());
    }

    public record SubmitResponse(boolean ok, String applicationNo) {}
}
