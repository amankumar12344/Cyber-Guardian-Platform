package com.guardian;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.SecurityService;
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
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class Main {
    private static List<String> blacklist = new ArrayList<>();
    private static int scanInterval = 2000;
    private static String masterPassword = "admin";
    private static boolean isLocked = true;
    private static String botToken = "";
    private static String chatId = "";
    private static long lastUpdateId = 0;

    private static TrayIcon trayIcon;
    private static Dashboard dashboard;
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static SecurityService securityService;

    public static boolean isLocked() { return isLocked; }
    public static void setLockedRemote(boolean locked) {
        if (locked) remoteLock(); else remoteUnlock();
    }

    public static void main(String[] args) {
        // Fix for SQLite AccessDeniedException: C:\Windows\TEMP
        try {
            java.io.File tempDir = new java.io.File("temp");
            if (!tempDir.exists()) tempDir.mkdirs();
            System.setProperty("org.sqlite.tmpdir", tempDir.getAbsolutePath());
            System.out.println("📂 [DEBUG] Local Temp Directory Set: " + tempDir.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("⚠️ [DEBUG] Could not set local temp directory: " + e.getMessage());
        }

        System.out.println("🚀 [DEBUG] Booting Cyber Guardian...");
        System.out.flush();

        ConfigurableApplicationContext context = null;
        try {
            context = new SpringApplicationBuilder(Main.class)
                    .headless(false)
                    .run(args);
            System.out.println("✅ [DEBUG] Spring Context Started Successfully.");
            setupSystemTray();
            System.out.println("🖼️ [DEBUG] System Tray Initialized.");
            System.out.flush();
        } catch (Exception e) {
            System.err.println("❌ [DEBUG] Spring Boot Failed to Start:");
            e.printStackTrace();
            System.out.flush();
            System.exit(1);
        }

        final ConfigurableApplicationContext finalContext = context;
        try {
            UserRepository userRepo = finalContext.getBean(UserRepository.class);
            long userCount = userRepo.count();
            System.out.println("✅ [DEBUG] Database Connected. User Count: " + userCount);
            System.out.flush();
            
            if (userCount == 0) {
                isLocked = false;
            }

            securityService = finalContext.getBean(SecurityService.class);
            loadConfig(finalContext);
            securityService.setBlacklist(blacklist);
            securityService.setLocked(isLocked);
            System.out.println("✅ [DEBUG] Security Service Initialized.");
            System.out.flush();
            
            securityService.setAlertCallback(appName -> {
                String screenshotName = "alert_" + System.currentTimeMillis() + ".jpg";
                String screenshotPath = takeScreenshot(screenshotName);
                
                // Save log to DB with screenshot path
                com.guardian.repository.LogRepository logRepo = finalContext.getBean(com.guardian.repository.LogRepository.class);
                logRepo.save(new com.guardian.entity.LogEntry(appName, "TERMINATED", "App blocked and screenshot captured.", screenshotName));

                // Send Telegram (with fallback to official bot if needed)
                sendTelegramMessage("⚠️ ALERT: Someone tried to open *" + appName + "* on your PC! Access was blocked. 🚫");
            });

            SwingUtilities.invokeLater(() -> {
                try {
                    System.out.println("🔍 [DEBUG] Checking UI State...");
                    if (userRepo.count() == 0) {
                        System.out.println("🚀 [DEBUG] Setup Mode Active. Opening browser...");
                    // Auto-open browser to welcome page
                    try {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(new URI("http://localhost:8081/welcome"));
                            System.out.println("🌐 Browser opened: http://localhost:8081/welcome");
                        }
                    } catch (Exception ex) {
                        System.out.println("🌐 Please open: http://localhost:8081/welcome");
                    }
                    } else {
                        System.out.println("📊 [DEBUG] Launching Dashboard...");
                        loadConfig(finalContext); // Refresh password from DB before creating dashboard
                        dashboard = new Dashboard(isLocked, botToken, chatId, masterPassword);
                        dashboard.setOnStatusChange(locked -> {
                            if (locked) remoteLock(); else remoteUnlock();
                        });
                        dashboard.setOnSettingsSave(settings -> {
                            System.out.println("💾 [DEBUG] Saving new settings...");
                            botToken = settings.getString("botToken");
                            chatId = settings.getString("chatId");
                            masterPassword = settings.getString("password");
                            saveConfigToFile();
                            System.out.println("✅ [DEBUG] Settings Saved and Applied.");
                            System.out.flush();
                        });
                        dashboard.setVisible(true);
                    }
                    System.out.flush();
                } catch (Exception e) {
                    System.err.println("❌ [DEBUG] UI Error: " + e.getMessage());
                }
            });

            System.out.println("📱 [DEBUG] Starting Telegram Listener...");
            System.out.flush();
            startTelegramListener(finalContext);

            System.out.println("🛡️ [DEBUG] Entering Security Loop...");
            System.out.flush();
            
            int syncCounter = 0;
            while (true) {
                securityService.scanAndProtect();
                
                if (dashboard == null && userRepo.count() > 0) {
                    SwingUtilities.invokeLater(() -> {
                        for (Window w : Window.getWindows()) {
                            if (w instanceof SetupWizard) w.dispose();
                        }
                        loadConfig(finalContext);
                        System.out.println("📊 [DEBUG] User detected! Launching Dashboard...");
                        dashboard = new Dashboard(isLocked, botToken, chatId, masterPassword);
                        
                        // ESSENTIAL: Set callbacks for the dynamic dashboard
                        dashboard.setOnStatusChange(locked -> {
                            if (locked) remoteLock(); else remoteUnlock();
                        });
                        dashboard.setOnSettingsSave(settings -> {
                            System.out.println("💾 [DEBUG] Saving new settings...");
                            botToken = settings.getString("botToken");
                            chatId = settings.getString("chatId");
                            masterPassword = settings.getString("password");
                            saveConfigToFile();
                            System.out.println("✅ [DEBUG] Settings Saved and Applied.");
                            System.out.flush();
                        });
                        
                        dashboard.setVisible(true);
                        System.out.println("✅ [DEBUG] Dashboard Initialized with Callbacks.");
                        System.out.flush();
                    });
                }

                if (syncCounter >= 5) {
                    loadConfig(finalContext);
                    syncCounter = 0;
                }
                syncCounter++;
                Thread.sleep(scanInterval);
            }
        } catch (Throwable t) {
            System.err.println("❌ [DEBUG] CRITICAL RUNTIME ERROR:");
            t.printStackTrace();
            System.out.flush();
            // Prevent instant exit so user can read error
            try { Thread.sleep(60000); } catch (Exception ignored) {}
        }
    }

    private static void loadConfig(ConfigurableApplicationContext context) {
        try {
            UserRepository userRepo = context.getBean(UserRepository.class);
            List<User> users = userRepo.findAll();
            for (User user : users) {
                if (user.getPassword() != null) {
                    masterPassword = user.getPassword();
                }
                if (user.getTelegramBotToken() != null && !user.getTelegramBotToken().isEmpty()) {
                    botToken = user.getTelegramBotToken();
                    chatId = user.getTelegramChatId();
                }
            }
            if (Files.exists(Paths.get("config.json"))) {
                String content = new String(Files.readAllBytes(Paths.get("config.json")));
                JSONObject config = new JSONObject(content);
                if (config.has("masterPassword")) masterPassword = config.getString("masterPassword");
                else if (config.has("password")) masterPassword = config.getString("password");

                if (dashboard != null) dashboard.updatePassword(masterPassword);

                JSONArray bl = config.getJSONArray("blacklist");
                blacklist.clear();
                for (int i = 0; i < bl.length(); i++) blacklist.add(bl.getString(i));
            }
        } catch (Exception ignored) {}
    }

    private static final String PLATFORM_BOT_TOKEN = "8780573988:AAEAWFDtYg_p-hst4JwqA9RSWN9cNzW7eKk"; // Official Platform Bot

    private static void startTelegramListener(ConfigurableApplicationContext context) {
        new Thread(() -> {
            System.out.println("📱 Official Telegram Listener ACTIVE...");
            while (true) {
                try {
                    // Always listen using the Platform Bot Token
                    String url = "https://api.telegram.org/bot" + PLATFORM_BOT_TOKEN + "/getUpdates?offset=" + (lastUpdateId + 1);
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        JSONArray results = json.getJSONArray("result");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject update = results.getJSONObject(i);
                            lastUpdateId = update.getLong("update_id");
                            JSONObject msg = update.optJSONObject("message");
                            if (msg != null && msg.has("text")) {
                                String text = msg.getString("text");
                                long currentChatId = msg.getJSONObject("chat").getLong("id");
                                handleCommand(text, String.valueOf(currentChatId), context);
                            }
                        }
                    }
                } catch (Exception ignored) {}
                try { Thread.sleep(2000); } catch (Exception ignored) {}
            }
        }).start();
    }

    private static void handleCommand(String command, String senderChatId, ConfigurableApplicationContext context) {
        try {
            UserRepository userRepo = context.getBean(UserRepository.class);
            
            // AUTO-LINK LOGIC: If user sends /start email@domain.com
            if (command.startsWith("/start")) {
                String[] parts = command.split(" ");
                if (parts.length > 1) {
                    String email = parts[1];
                    java.util.Optional<User> userOpt = userRepo.findByEmail(email);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setTelegramBotToken(PLATFORM_BOT_TOKEN);
                        user.setTelegramChatId(senderChatId);
                        userRepo.save(user);
                        
                        botToken = PLATFORM_BOT_TOKEN;
                        chatId = senderChatId;
                        
                        sendTelegramMessage("✅ *Telegram Connected!* \n\n" +
                            "🛡️ Your account (" + email + ") is now linked to Cyber Guardian.\n" +
                            "You will receive all security alerts and screenshots here.", senderChatId);
                        return;
                    }
                }
                sendTelegramMessage("👋 *Welcome to Cyber Guardian!* \nTo link your account, please use the 'Link Telegram' button in your dashboard or extension.", senderChatId);
                return;
            }

            // Normal Commands
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            
            // Verify if this chatId is authorized
            if (!senderChatId.equals(chatId)) {
                sendTelegramMessage("⚠️ *Unauthorized Access!* Please link your account first.", senderChatId);
                return;
            }
            
            if (command.equals("/lock")) {
                remoteLock();
                sendTelegramMessage("🔐 *System LOCKED successfully.* Monitoring active. 🛡️");
            }
            else if (command.equals("/unlock")) {
                remoteUnlock();
                sendTelegramMessage("🔓 *System UNLOCKED successfully.*");
            }
            else if (command.equals("/screenshot")) {
                sendTelegramMessage("📸 *Capturing screenshot...*");
                takeScreenshot("manual_" + System.currentTimeMillis() + ".jpg");
            }
            else if (command.equals("/shutdown")) {
                sendTelegramMessage("🔌 *Shutting down the PC...* Goodbye! 👋");
                if (isWindows) Runtime.getRuntime().exec("shutdown /s /t 5");
                else Runtime.getRuntime().exec("shutdown now");
            }
            else if (command.equals("/restart")) {
                sendTelegramMessage("🔄 *Restarting the PC...* Please wait.");
                if (isWindows) Runtime.getRuntime().exec("shutdown /r /t 5");
                else Runtime.getRuntime().exec("reboot");
            }
            else if (command.equals("/sleep")) {
                sendTelegramMessage("💤 *PC is going to Sleep mode...*");
                if (isWindows) Runtime.getRuntime().exec("rundll32.exe powrprof.dll,SetSuspendState 0,1,0");
                else Runtime.getRuntime().exec("systemctl suspend");
            }
            else if (command.startsWith("/record")) {
                int secs = 10;
                try { secs = Integer.parseInt(command.split(" ")[1]); } catch (Exception ignored) {}
                sendTelegramMessage("🎥 *Started recording " + secs + " seconds video...*");
                recordVideo(secs);
            }
        } catch (Exception e) {
            System.err.println("Command execution error: " + e.getMessage());
            sendTelegramMessage("❌ *Command Failed:* " + e.getMessage());
        }
    }

    private static String takeScreenshot(String fileName) {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            java.awt.image.BufferedImage img = robot.createScreenCapture(screenRect);
            
            // Create screenshots directory if it doesn't exist
            java.io.File dir = new java.io.File("screenshots");
            if (!dir.exists()) dir.mkdirs();
            
            java.io.File file = new java.io.File(dir, fileName);
            
            java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
            javax.imageio.ImageWriter writer = writers.next();
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.75f);
            
            try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(file)) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
            }
            writer.dispose();
            sendTelegramPhoto(file);
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }

    private static void sendTelegramMessage(String text) {
        sendTelegramMessage(text, chatId);
    }

    private static void sendTelegramMessage(String text, String targetChatId) {
        new Thread(() -> {
            try {
                if (PLATFORM_BOT_TOKEN == null || PLATFORM_BOT_TOKEN.isEmpty()) return;
                if (targetChatId == null || targetChatId.isEmpty()) return;
                
                String url = "https://api.telegram.org/bot" + PLATFORM_BOT_TOKEN + "/sendMessage";
                JSONObject json = new JSONObject();
                json.put("chat_id", targetChatId);
                json.put("text", text);
                json.put("parse_mode", "Markdown");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        }).start();
    }

    private static void sendTelegramPhoto(java.io.File file) {
        new Thread(() -> {
            try {
                if (PLATFORM_BOT_TOKEN == null || chatId == null || chatId.isEmpty()) return;
                String url = "https://api.telegram.org/bot" + PLATFORM_BOT_TOKEN + "/sendPhoto";
                String boundary = "---" + System.currentTimeMillis();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(file, boundary, "photo", "image/jpeg"))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        }).start();
    }

    private static void recordVideo(int seconds) {
        new Thread(() -> {
            try {
                System.out.println("🎥 Recording started for " + seconds + " seconds...");
                java.io.File file = new java.io.File("record.mp4");
                org.jcodec.api.awt.AWTSequenceEncoder encoder = org.jcodec.api.awt.AWTSequenceEncoder.createSequenceEncoder(file, 8); // 8 FPS is enough
                
                Robot robot = new Robot();
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle screenRect = new Rectangle(screen);
                
                int targetWidth = screen.width / 2; // Scale down for speed
                int targetHeight = screen.height / 2;
                
                long endTime = System.currentTimeMillis() + (seconds * 1000L);
                while (System.currentTimeMillis() < endTime) {
                    long frameStart = System.currentTimeMillis();
                    java.awt.image.BufferedImage img = robot.createScreenCapture(screenRect);
                    
                    // Resize for speed and size
                    java.awt.image.BufferedImage scaledImg = new java.awt.image.BufferedImage(targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = scaledImg.createGraphics();
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(img, 0, 0, targetWidth, targetHeight, null);
                    g.dispose();
                    
                    encoder.encodeImage(scaledImg);
                    
                    long elapsed = System.currentTimeMillis() - frameStart;
                    if (elapsed < 125) Thread.sleep(125 - elapsed); // Aim for 8 FPS
                }
                encoder.finish();
                System.out.println("🎥 Recording finished: " + file.getAbsolutePath());
                sendTelegramVideo(file);
            } catch (Exception e) {
                System.err.println("Video record error: " + e.getMessage());
            }
        }).start();
    }

    private static void sendTelegramVideo(java.io.File file) {
        new Thread(() -> {
            try {
                String url = "https://api.telegram.org/bot" + botToken + "/sendVideo";
                String boundary = "---" + System.currentTimeMillis();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(buildMultipartBody(file, boundary, "video", "video/mp4"))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        }).start();
    }

    private static HttpRequest.BodyPublisher buildMultipartBody(java.io.File file, String boundary, String fieldName, String contentType) throws IOException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String head = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + chatId + "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n";
        String foot = "\r\n--" + boundary + "--\r\n";
        byte[] headBytes = head.getBytes(StandardCharsets.UTF_8);
        byte[] footBytes = foot.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[headBytes.length + fileBytes.length + footBytes.length];
        System.arraycopy(headBytes, 0, combined, 0, headBytes.length);
        System.arraycopy(fileBytes, 0, combined, headBytes.length, fileBytes.length);
        System.arraycopy(footBytes, 0, combined, headBytes.length + fileBytes.length, footBytes.length);
        return HttpRequest.BodyPublishers.ofByteArray(combined);
    }

    private static void remoteLock() {
        isLocked = true;
        securityService.setLocked(true);
        if (dashboard != null) dashboard.updateStatus(true);
    }

    private static void remoteUnlock() {
        isLocked = false;
        securityService.setLocked(false);
        if (dashboard != null) dashboard.updateStatus(false);
    }

    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png"));
            PopupMenu popup = new PopupMenu();
            
            MenuItem openItem = new MenuItem("Open Dashboard");
            openItem.addActionListener(e -> {
                if (dashboard != null) {
                    dashboard.setVisible(true);
                    dashboard.toFront();
                }
            });
            popup.add(openItem);
            
            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Exit Guardian");
            exitItem.addActionListener(e -> System.exit(0));
            popup.add(exitItem);
            trayIcon = new TrayIcon(image, "Cyber Guardian", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
        } catch (Exception e) {
            System.err.println("❌ [DEBUG] System Tray Error: " + e.getMessage());
        }
    }

    private static void saveConfigToFile() {
        try {
            JSONObject config = new JSONObject();
            config.put("botToken", botToken);
            config.put("chatId", chatId);
            config.put("masterPassword", masterPassword);
            config.put("scanInterval", scanInterval);
            
            JSONArray bl = new JSONArray();
            for (String p : blacklist) bl.put(p);
            config.put("blacklist", bl);

            Files.write(Paths.get("config.json"), config.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("❌ [DEBUG] Failed to save config: " + e.getMessage());
        }
    }
}
