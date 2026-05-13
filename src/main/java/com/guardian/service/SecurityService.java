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
    private java.util.function.Consumer<String> alertCallback;

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
                            // 1. Show On-Screen Warning
                            showWarningPopup(app.toUpperCase());

                            // 2. Trigger Alert Callback (Main.java handles logging + screenshots + telegram)
                            if (alertCallback != null) alertCallback.accept(app.toUpperCase());
                        }
                    }
                }
            });
    }

    private void showWarningPopup(String appName) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame();
            frame.setUndecorated(true);
            frame.setAlwaysOnTop(true);
            frame.setSize(400, 100);
            frame.setLocationRelativeTo(null);
            frame.setBackground(new java.awt.Color(150, 0, 0, 200));

            javax.swing.JLabel label = new javax.swing.JLabel("🚫 ACCESS DENIED: " + appName + " is blocked!", javax.swing.SwingConstants.CENTER);
            label.setForeground(java.awt.Color.WHITE);
            label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
            frame.add(label);

            frame.setVisible(true);
            new javax.swing.Timer(3000, e -> frame.dispose()).start();
        });
    }

    public List<LogEntry> getAllLogs() {
        return logRepository.findAll();
    }
}
