package com.guardian.controller;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*") // Allows requests from Vercel
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private final java.util.concurrent.ConcurrentHashMap<String, String> otpStorage = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/api/forgot-password")
    public Map<String, Object> verifyReset(@RequestParam String email, @RequestParam String phone) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPhoneNumber().equals(phone)) {
            response.put("success", true);
            response.put("email", email);
            return response;
        }
        response.put("success", false);
        response.put("message", "Email or Phone Number is incorrect!");
        return response;
    }

    @PostMapping("/api/reset-password")
    public Map<String, Object> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            User u = user.get();
            u.setPassword(newPassword);
            userRepository.save(u);
            response.put("success", true);
            response.put("message", "Password reset successful!");
            return response;
        }
        response.put("success", false);
        response.put("message", "User not found!");
        return response;
    }

    @PostMapping("/api/send-otp")
    public Map<String, Object> sendOtp(@RequestParam String identifier) {
        Map<String, Object> response = new HashMap<>();
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.put("success", false);
            response.put("message", "Invalid email format!");
            return response;
        }
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
        otpStorage.put(identifier, otp);
        System.out.println("[TESTING] Generated OTP for " + identifier + " is: " + otp);

        if (identifier.contains("@")) {
            try {
                emailService.sendOtpEmail(identifier, otp);
                response.put("success", true);
                response.put("message", "OTP Sent Successfully to Email!");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Error sending Email: " + e.getMessage());
            }
        } else {
            try {
                String botToken = "8780573988:AAEAWFDtYg_p-hst4JwqA9RSWN9cNzW7eKk";
                String chatId = "1123697239";
                String text = "🛡️ Guardian Security Code: " + otp + "\nRequested for: " + identifier;
                String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(text, "UTF-8");

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getInputStream().close();

                response.put("success", true);
                response.put("message", "OTP Sent Successfully to Telegram!");
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Error sending Telegram OTP: " + e.getMessage());
            }
        }
        return response;
    }

    @PostMapping("/api/signup")
    public Map<String, Object> registerUser(@RequestParam String identifier, @RequestParam String password, @RequestParam String otp) {
        Map<String, Object> response = new HashMap<>();
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.put("success", false);
            response.put("message", "Invalid email format!");
            return response;
        }
        String storedOtp = otpStorage.get(identifier);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            response.put("success", false);
            response.put("message", "Invalid or expired OTP!");
            return response;
        }
        if (userRepository.findByEmail(identifier).isPresent()) {
            response.put("success", false);
            response.put("message", "User already exists!");
            return response;
        }
        User user = new User(identifier, password);
        user.setRole("ADMIN");
        if (identifier.contains("@")) {
            user.setPhoneNumber(null);
        } else {
            user.setPhoneNumber(identifier);
        }
        userRepository.save(user);
        otpStorage.remove(identifier);
        
        response.put("success", true);
        response.put("message", "Account Created successfully!");
        return response;
    }

    @PostMapping("/api/register")
    public java.util.Map<String, Object> apiRegister(@RequestParam String email, @RequestParam String password, 
                                                   @RequestParam(required = false) String botToken, 
                                                   @RequestParam(required = false) String chatId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (userRepository.findByEmail(email).isPresent()) {
            response.put("success", false);
            response.put("message", "User already exists!");
            return response;
        }
        
        User user = new User(email, password);
        user.setRole("ADMIN");
        if (botToken != null) user.setTelegramBotToken(botToken);
        if (chatId != null) user.setTelegramChatId(chatId);
        
        userRepository.save(user);
        response.put("success", true);
        response.put("message", "Account Created successfully!");
        return response;
    }

    @PostMapping("/api/login")
    public Map<String, Object> loginUser(@RequestParam String identifier, @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> user = userRepository.findByEmail(identifier);
        
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            String role = user.get().getRole();
            if (role == null) role = "ADMIN";
            response.put("success", true);
            response.put("apiKey", user.get().getApiKey());
            response.put("role", role);
            return response;
        }
        response.put("success", false);
        response.put("message", "Invalid Credentials!");
        return response;
    }

    @PostMapping("/api/police/signup")
    public Map<String, Object> registerPoliceUser(@RequestParam String identifier, @RequestParam String password, @RequestParam(required = false) String otp) {
        Map<String, Object> response = new HashMap<>();
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            response.put("success", false);
            response.put("message", "Invalid email format!");
            return response;
        }
        if (otp != null && !otp.equals(otpStorage.get(identifier))) {
            response.put("success", false);
            response.put("message", "Invalid or expired OTP!");
            return response;
        }
        if (userRepository.findByEmail(identifier).isPresent()) {
            response.put("success", false);
            response.put("message", "Officer already registered!");
            return response;
        }
        User user = new User(identifier, password);
        user.setRole("POLICE");
        user.setPhoneNumber(identifier); // just for storing
        userRepository.save(user);
        if (otp != null) otpStorage.remove(identifier);
        
        response.put("success", true);
        response.put("message", "Police account registered successfully!");
        return response;
    }

    @PostMapping("/api/police/login")
    public Map<String, Object> loginPoliceUser(@RequestParam String identifier, @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            String role = user.get().getRole();
            if (role == null) role = "ADMIN";
            response.put("success", true);
            response.put("apiKey", user.get().getApiKey());
            response.put("role", role);
            return response;
        }
        response.put("success", false);
        response.put("message", "Invalid Credentials!");
        return response;
    }
}
