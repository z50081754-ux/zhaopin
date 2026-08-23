package com.xw.recruitment.job;

import com.xw.recruitment.config.VisitorRegionResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class PublicJobController {
    private final JobService service;
    private final VisitorRegionResolver regionResolver;

    public PublicJobController(JobService service, VisitorRegionResolver regionResolver) {
        this.service = service;
        this.regionResolver = regionResolver;
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> list(HttpServletRequest request) {
        boolean international = !regionResolver.usesSoutheastAsiaSalary(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicJobs(international));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<JobResponse> detail(@PathVariable String slug, HttpServletRequest request) {
        boolean international = !regionResolver.usesSoutheastAsiaSalary(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicJob(slug, international));
    }
}
