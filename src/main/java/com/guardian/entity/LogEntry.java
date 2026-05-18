package com.guardian.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_logs")
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appName;
    private String action;
    private String details;
    private String screenshotPath;
    private String targetId;
    private LocalDateTime timestamp;

    public LogEntry() {}

    public LogEntry(String appName, String action, String details, String screenshotPath, String targetId) {
        this.appName = appName;
        this.action = action;
        this.details = details;
        this.screenshotPath = screenshotPath;
        this.targetId = targetId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getAppName() { return appName; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getScreenshotPath() { return screenshotPath; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
