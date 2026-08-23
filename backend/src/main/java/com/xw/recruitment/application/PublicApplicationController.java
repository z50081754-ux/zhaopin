package com.xw.recruitment.application;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class PublicApplicationController {
    private final ApplicationService service;

    public PublicApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitResponse submit(@Valid @ModelAttribute ApplicationRequest request) {
        ApplicationEntity saved = service.submit(request);
        return new SubmitResponse(true, saved.getApplicationNo());
    }

    public record SubmitResponse(boolean ok, String applicationNo) {}
}
