package com.guardian.service;

import com.guardian.entity.LogEntry;
import com.guardian.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class SecurityService {

    @Autowired
    private LogRepository logRepository;

    private List<String> blacklist = new ArrayList<>();
    private boolean isLocked = true;
    private boolean isKavachActive = false;
    private java.util.function.Consumer<String> alertCallback;

    public void setKavachActive(boolean active) {
        this.isKavachActive = active;
    }

    public boolean isKavachActive() {
        return isKavachActive;
    }

    public void addLog(String appName, String action, String details, String screenshotPath, String targetId) {
        LogEntry log = new LogEntry(appName, action, details, screenshotPath, targetId);
        logRepository.save(log);
    }

    public List<LogEntry> getLogsByTargetId(String targetId) {
        if (targetId == null || targetId.isEmpty() || "ALL".equals(targetId)) {
            return logRepository.findAll();
        }
        return logRepository.findByTargetId(targetId);
    }

    public List<String> getAllTargetIds() {
        return logRepository.findDistinctTargetIds();
    }

    public void setBlacklist(List<String> list) {
        this.blacklist = list;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }

    public void setAlertCallback(java.util.function.Consumer<String> callback) {
        this.alertCallback = callback;
    }

    public void scanAndProtect() {
        if (!isLocked) return;

        ProcessHandle.allProcesses()
            .filter(ph -> ph.info().command().isPresent())
            .forEach(ph -> {
                String cmd = ph.info().command().get().toLowerCase();
                for (String app : blacklist) {
                    if (cmd.contains(app.toLowerCase())) {
                        boolean killed = ph.destroy();
                        if (killed) {
                            // Trigger Alert Callback (Main.java handles logging + screenshots + telegram)
                            if (alertCallback != null) alertCallback.accept(app.toUpperCase());
                        }
                    }
                }
            });
    }

    public List<LogEntry> getAllLogs() {
        return logRepository.findAll();
    }

    public void clearAllLogs() {
        logRepository.deleteAll();
    }
}
