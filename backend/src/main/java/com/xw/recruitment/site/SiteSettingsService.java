package com.xw.recruitment.site;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Set;

@Service
public class SiteSettingsService {
    private static final long SETTINGS_ID = 1L;
    private static final Set<String> TEMPLATES = Set.of("technology", "apple");
    private static final Set<String> LANGUAGES = Set.of("auto", "zh", "en");
    private final SiteSettingsRepository repository;

    public SiteSettingsService(SiteSettingsRepository repository) { this.repository = repository; }

    public SiteSettingsResponse get() {
        return repository.findById(SETTINGS_ID).map(SiteSettingsResponse::from)
            .orElseGet(() -> new SiteSettingsResponse("technology", "auto", null));
    }

    @Transactional
    public SiteSettingsResponse update(String template, String defaultLanguage) {
        if (!TEMPLATES.contains(template)) throw new IllegalArgumentException("Invalid site template.");
        if (!LANGUAGES.contains(defaultLanguage)) throw new IllegalArgumentException("Invalid default language.");
        SiteSettingsEntity entity = repository.findById(SETTINGS_ID)
            .orElseGet(() -> new SiteSettingsEntity(SETTINGS_ID, "technology", "auto", Instant.now()));
        entity.setActiveTemplate(template);
        entity.setDefaultLanguage(defaultLanguage);
        entity.setUpdatedAt(Instant.now());
        return SiteSettingsResponse.from(repository.save(entity));
    }

    public String resolveInitialLanguage(String country) {
        String configured = get().defaultLanguage();
        if ("zh".equals(configured) || "en".equals(configured)) return configured;
        return "CN".equals(country) || "LOCAL".equals(country) ? "zh" : "en";
    }
}
