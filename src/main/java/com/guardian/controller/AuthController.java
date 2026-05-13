package com.guardian.controller;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@CrossOrigin
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private String generatedOtp = null;
    private String otpRecipient = null;

    @GetMapping("/")
    public String index() {
        if (userRepository.count() == 0) {
            return "redirect:/signup";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String verifyReset(@RequestParam String email, @RequestParam String phone, Model model) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPhoneNumber().equals(phone)) {
            model.addAttribute("email", email);
            return "reset-password";
        }
        model.addAttribute("error", "Email or Phone Number is incorrect!");
        return "forgot-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            User u = user.get();
            u.setPassword(newPassword);
            userRepository.save(u);
            return "redirect:/login?resetSuccess";
        }
        return "redirect:/login";
    }

    @GetMapping("/welcome")
    public String showWelcomePage() {
        return "welcome";
    }

    @GetMapping("/signup")
    public String showSignupPage() {
        return "signup";
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public String sendOtp(@RequestParam String identifier) {
        generatedOtp = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
        otpRecipient = identifier;

        if (identifier.contains("@")) {
            // Send OTP via Email
            try {
                emailService.sendOtpEmail(identifier, generatedOtp);
                return "OTP Sent Successfully to Email!";
            } catch (Exception e) {
                return "Error sending Email: " + e.getMessage();
            }
        } else {
            // Send OTP via Telegram
            try {
                String botToken = "8780573988:AAEAWFDtYg_p-hst4JwqA9RSWN9cNzW7eKk";
                String chatId = "1123697239";
                String text = "🛡️ Guardian Security Code: " + generatedOtp + "\nRequested for: " + identifier;
                String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + java.net.URLEncoder.encode(text, "UTF-8");

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.getInputStream().close();

                return "OTP Sent Successfully to Telegram!";
            } catch (Exception e) {
                return "Error sending Telegram OTP: " + e.getMessage();
            }
        }
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam String identifier, @RequestParam String password, Model model) {
        if (userRepository.findByEmail(identifier).isPresent()) {
            model.addAttribute("error", "User with this Email/Phone already exists!");
            return "signup";
        }
        User user = new User(identifier, password);
        if (identifier.contains("@")) {
            user.setPhoneNumber(null);
        } else {
            user.setPhoneNumber(identifier);
        }
        userRepository.save(user);
        return "redirect:/login?success";
    }

    @PostMapping("/api/register")
    @ResponseBody
    @CrossOrigin
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
        if (botToken != null) user.setTelegramBotToken(botToken);
        if (chatId != null) user.setTelegramChatId(chatId);
        
        userRepository.save(user);
        response.put("success", true);
        response.put("message", "Account Created successfully!");
        return response;
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String identifier, @RequestParam String password, Model model) {
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return "redirect:/dashboard?apiKey=" + user.get().getApiKey();
        }
        model.addAttribute("error", "Invalid Credentials!");
        return "login";
    }
}
