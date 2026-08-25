package com.xw.recruitment.site;

import java.time.Instant;

public record SiteSettingsResponse(String activeTemplate, Instant updatedAt) {
    static SiteSettingsResponse from(SiteSettingsEntity entity) {
        return new SiteSettingsResponse(entity.getActiveTemplate(), entity.getUpdatedAt());
    }
}
