package com.xw.recruitment.research;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "research_campaign")
public class ResearchCampaignEntity {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "terms_version", nullable = false, length = 40)
    private String termsVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ResearchCampaignEntity() {}

    public Long getId() { return id; }
    public String getStatus() { return status; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getUpdatedAt() { return updatedAt; }
}
