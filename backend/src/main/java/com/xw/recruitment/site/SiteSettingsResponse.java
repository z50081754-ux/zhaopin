package com.xw.recruitment.site;

import java.time.Instant;

public record SiteSettingsResponse(String activeTemplate, String defaultLanguage, Instant updatedAt) {
    static SiteSettingsResponse from(SiteSettingsEntity entity) {
        return new SiteSettingsResponse(entity.getActiveTemplate(), entity.getDefaultLanguage(), entity.getUpdatedAt());
    }
}
