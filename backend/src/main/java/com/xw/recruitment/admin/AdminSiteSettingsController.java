package com.xw.recruitment.admin;

import com.xw.recruitment.site.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/site-settings")
public class AdminSiteSettingsController {
    private final SiteSettingsService service;
    public AdminSiteSettingsController(SiteSettingsService service) { this.service = service; }

    @GetMapping
    public SiteSettingsResponse get() { return service.get(); }

    @PutMapping
    public SiteSettingsResponse update(@RequestBody UpdateRequest request) {
        return service.update(request.activeTemplate(), request.defaultLanguage());
    }

    public record UpdateRequest(String activeTemplate, String defaultLanguage) {}
}
