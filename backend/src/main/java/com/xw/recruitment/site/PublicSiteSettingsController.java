package com.xw.recruitment.site;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-settings")
public class PublicSiteSettingsController {
    private final SiteSettingsService service;
    public PublicSiteSettingsController(SiteSettingsService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<SiteSettingsResponse> get() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get());
    }
}
