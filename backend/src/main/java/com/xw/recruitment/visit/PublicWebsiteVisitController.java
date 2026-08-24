package com.xw.recruitment.visit;

import com.xw.recruitment.config.VisitorRegionResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/visits")
public class PublicWebsiteVisitController {
    private final WebsiteVisitService service;
    private final VisitorRegionResolver regionResolver;

    public PublicWebsiteVisitController(WebsiteVisitService service, VisitorRegionResolver regionResolver) {
        this.service = service;
        this.regionResolver = regionResolver;
    }

    @PostMapping
    public Map<String, Boolean> qualify(
        @RequestBody WebsiteVisitService.VisitRequest request,
        HttpServletRequest httpRequest
    ) {
        service.qualify(request, regionResolver.ipAddress(httpRequest));
        return Map.of("ok", true);
    }

    @PostMapping("/{visitId}/heartbeat")
    public Map<String, Boolean> heartbeat(
        @PathVariable String visitId,
        @RequestBody WebsiteVisitService.HeartbeatRequest request
    ) {
        service.heartbeat(visitId, request);
        return Map.of("ok", true);
    }
}
