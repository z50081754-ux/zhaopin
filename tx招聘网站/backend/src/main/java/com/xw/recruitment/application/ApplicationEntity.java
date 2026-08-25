package com.xw.recruitment.application;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "applications")
public class ApplicationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String applicationNo;
    private String resumeName;
    private String telegram;
    private String gender;
    private String ageAtApplication;
    private String birthDate;
    private String nationality;
    private String jobTitle;
    private String referrer;
    @Column(length = 2000)
    private String remarks;
    private String currentSalary;
    private String expectedSalary;
    private String bcExperience;
    private String employmentStatus;
    private String educationType;
    private String school;
    private String educationPeriod;
    private String passportStatus;
    private String visaStatus;
    private String interviewTime;
    private String startTime;
    private String currentCountry;
    private String preferredCountry;
    private String ipAddress;
    private String deviceType;
    private String deviceModel;
    private String operatingSystem;
    private String browserName;
    private String screenResolution;
    private String deviceLanguage;
    private String deviceTimezone;
    @Column(length = 1000)
    private String userAgent;
    private String stage;
    private boolean possibleDuplicate;
    private String resumeStorageKey;
    private String resumeOriginalFilename;
    private String resumeContentType;
    private long resumeSize;
    private String privacyPolicyVersion;
    private Instant consentedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected ApplicationEntity() {}

    public Long getId() { return id; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String value) { applicationNo = value; }
    public String getResumeName() { return resumeName; }
    public void setResumeName(String value) { resumeName = value; }
    public String getTelegram() { return telegram; }
    public void setTelegram(String value) { telegram = value; }
    public String getGender() { return gender; }
    public void setGender(String value) { gender = value; }
    public String getAgeAtApplication() { return ageAtApplication; }
    public void setAgeAtApplication(String value) { ageAtApplication = value; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String value) { birthDate = value; }
    public String getNationality() { return nationality; }
    public void setNationality(String value) { nationality = value; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String value) { jobTitle = value; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String value) { referrer = value; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String value) { remarks = value; }
    public String getCurrentSalary() { return currentSalary; }
    public void setCurrentSalary(String value) { currentSalary = value; }
    public String getExpectedSalary() { return expectedSalary; }
    public void setExpectedSalary(String value) { expectedSalary = value; }
    public String getBcExperience() { return bcExperience; }
    public void setBcExperience(String value) { bcExperience = value; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String value) { employmentStatus = value; }
    public String getEducationType() { return educationType; }
    public void setEducationType(String value) { educationType = value; }
    public String getSchool() { return school; }
    public void setSchool(String value) { school = value; }
    public String getEducationPeriod() { return educationPeriod; }
    public void setEducationPeriod(String value) { educationPeriod = value; }
    public String getPassportStatus() { return passportStatus; }
    public void setPassportStatus(String value) { passportStatus = value; }
    public String getVisaStatus() { return visaStatus; }
    public void setVisaStatus(String value) { visaStatus = value; }
    public String getInterviewTime() { return interviewTime; }
    public void setInterviewTime(String value) { interviewTime = value; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String value) { startTime = value; }
    public String getCurrentCountry() { return currentCountry; }
    public void setCurrentCountry(String value) { currentCountry = value; }
    public String getPreferredCountry() { return preferredCountry; }
    public void setPreferredCountry(String value) { preferredCountry = value; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String value) { ipAddress = value; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String value) { deviceType = value; }
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String value) { deviceModel = value; }
    public String getOperatingSystem() { return operatingSystem; }
    public void setOperatingSystem(String value) { operatingSystem = value; }
    public String getBrowserName() { return browserName; }
    public void setBrowserName(String value) { browserName = value; }
    public String getScreenResolution() { return screenResolution; }
    public void setScreenResolution(String value) { screenResolution = value; }
    public String getDeviceLanguage() { return deviceLanguage; }
    public void setDeviceLanguage(String value) { deviceLanguage = value; }
    public String getDeviceTimezone() { return deviceTimezone; }
    public void setDeviceTimezone(String value) { deviceTimezone = value; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String value) { userAgent = value; }
    public String getStage() { return stage; }
    public void setStage(String value) { stage = value; }
    public boolean isPossibleDuplicate() { return possibleDuplicate; }
    public void setPossibleDuplicate(boolean value) { possibleDuplicate = value; }
    public String getResumeStorageKey() { return resumeStorageKey; }
    public void setResumeStorageKey(String value) { resumeStorageKey = value; }
    public String getResumeOriginalFilename() { return resumeOriginalFilename; }
    public void setResumeOriginalFilename(String value) { resumeOriginalFilename = value; }
    public String getResumeContentType() { return resumeContentType; }
    public void setResumeContentType(String value) { resumeContentType = value; }
    public long getResumeSize() { return resumeSize; }
    public void setResumeSize(long value) { resumeSize = value; }
    public String getPrivacyPolicyVersion() { return privacyPolicyVersion; }
    public void setPrivacyPolicyVersion(String value) { privacyPolicyVersion = value; }
    public Instant getConsentedAt() { return consentedAt; }
    public void setConsentedAt(Instant value) { consentedAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
