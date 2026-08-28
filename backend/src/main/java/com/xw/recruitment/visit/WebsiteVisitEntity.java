package com.xw.recruitment.visit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "website_visits")
public class WebsiteVisitEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String visitId;
    @Column(nullable = false)
    private Instant startedAt;
    @Column(nullable = false)
    private Instant qualifiedAt;
    @Column(nullable = false)
    private Instant lastSeenAt;
    @Column(nullable = false)
    private int durationSeconds;
    private String ipAddress;
    private String entryPath;
    private String lastPath;
    private String deviceType;
    private String deviceModel;
    private String operatingSystem;
    private String browserName;
    private String screenResolution;
    private String deviceLanguage;
    private String deviceTimezone;
    @Column(length = 1000)
    private String userAgent;
    @Column(length = 1000, nullable = false)
    private String detectedWallets = "";
    @Column(name = "system_code", nullable = false, length = 32)
    private String systemCode = "recruitment";
    @Column(name = "queried_address", nullable = false)
    private boolean queriedAddress = false;

    protected WebsiteVisitEntity() {}

    public Long getId() { return id; }
    public String getVisitId() { return visitId; }
    public void setVisitId(String value) { visitId = value; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant value) { startedAt = value; }
    public Instant getQualifiedAt() { return qualifiedAt; }
    public void setQualifiedAt(Instant value) { qualifiedAt = value; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant value) { lastSeenAt = value; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int value) { durationSeconds = value; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String value) { ipAddress = value; }
    public String getEntryPath() { return entryPath; }
    public void setEntryPath(String value) { entryPath = value; }
    public String getLastPath() { return lastPath; }
    public void setLastPath(String value) { lastPath = value; }
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
    public String getDetectedWallets() { return detectedWallets; }
    public void setDetectedWallets(String value) { detectedWallets = value; }
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String value) { systemCode = value; }
    public boolean isQueriedAddress() { return queriedAddress; }
    public void setQueriedAddress(boolean value) { queriedAddress = value; }
}
