package com.xw.recruitment.job;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String slug;
    private String title;
    private String category;
    private String businessUnit;
    private String requiredLocation;
    private String workMode;
    private String salaryRange;
    private String internationalSalaryRange;
    @Column(length = 1000)
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String responsibilities;
    @Column(columnDefinition = "TEXT")
    private String requirements;
    @Column(columnDefinition = "TEXT")
    private String bonus;
    private String status;
    private Integer recruitmentCount;
    private Instant createdAt;
    private Instant updatedAt;

    protected JobEntity() {}
    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String value) { businessUnit = value; }
    public String getRequiredLocation() { return requiredLocation; }
    public void setRequiredLocation(String value) { requiredLocation = value; }
    public String getWorkMode() { return workMode; }
    public void setWorkMode(String value) { workMode = value; }
    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String value) { salaryRange = value; }
    public String getInternationalSalaryRange() { return internationalSalaryRange; }
    public void setInternationalSalaryRange(String value) { internationalSalaryRange = value; }
    public String getSummary() { return summary; }
    public void setSummary(String value) { summary = value; }
    public String getResponsibilities() { return responsibilities; }
    public void setResponsibilities(String value) { responsibilities = value; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String value) { requirements = value; }
    public String getBonus() { return bonus; }
    public void setBonus(String value) { bonus = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getRecruitmentCount() { return recruitmentCount == null || recruitmentCount < 1 ? 1 : recruitmentCount; }
    public void setRecruitmentCount(Integer value) { recruitmentCount = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
