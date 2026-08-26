package com.xw.recruitment.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import com.xw.recruitment.site.SiteSettingsService;

@RestController
@RequestMapping("/api/locale")
public class LocaleController {
    private final VisitorRegionResolver regionResolver;
    private final SiteSettingsService siteSettingsService;

    public LocaleController(VisitorRegionResolver regionResolver, SiteSettingsService siteSettingsService) {
        this.regionResolver = regionResolver;
        this.siteSettingsService = siteSettingsService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> locale(HttpServletRequest request) {
        String country = regionResolver.country(request);
        String language = siteSettingsService.resolveInitialLanguage(country);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("country", country, "language", language));
    }

}
