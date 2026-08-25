package com.xw.recruitment.site;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Set;

@Service
public class SiteSettingsService {
    private static final long SETTINGS_ID = 1L;
    private static final Set<String> TEMPLATES = Set.of("technology", "apple");
    private final SiteSettingsRepository repository;

    public SiteSettingsService(SiteSettingsRepository repository) { this.repository = repository; }

    public SiteSettingsResponse get() {
        return repository.findById(SETTINGS_ID).map(SiteSettingsResponse::from)
            .orElseGet(() -> new SiteSettingsResponse("technology", null));
    }

    @Transactional
    public SiteSettingsResponse update(String template) {
        if (!TEMPLATES.contains(template)) throw new IllegalArgumentException("Invalid site template.");
        SiteSettingsEntity entity = repository.findById(SETTINGS_ID)
            .orElseGet(() -> new SiteSettingsEntity(SETTINGS_ID, "technology", Instant.now()));
        entity.setActiveTemplate(template);
        entity.setUpdatedAt(Instant.now());
        return SiteSettingsResponse.from(repository.save(entity));
    }
}
