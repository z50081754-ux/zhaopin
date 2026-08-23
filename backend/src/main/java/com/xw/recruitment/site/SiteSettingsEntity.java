package com.xw.recruitment.site;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "site_settings")
public class SiteSettingsEntity {
    @Id
    private Long id;
    @Column(nullable = false, length = 40)
    private String activeTemplate;
    @Column(nullable = false)
    private Instant updatedAt;

    protected SiteSettingsEntity() {}
    public SiteSettingsEntity(Long id, String activeTemplate, Instant updatedAt) {
        this.id = id;
        this.activeTemplate = activeTemplate;
        this.updatedAt = updatedAt;
    }
    public Long getId() { return id; }
    public String getActiveTemplate() { return activeTemplate; }
    public void setActiveTemplate(String value) { activeTemplate = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
