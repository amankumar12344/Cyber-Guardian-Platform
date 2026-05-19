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

    private final java.util.concurrent.ConcurrentHashMap<String, String> otpStorage = new java.util.concurrent.ConcurrentHashMap<>();

    @GetMapping("/")
    public String index() {
        return "welcome";
    }

    @GetMapping("/portal")
    public String showPortal() {
        return "portal";
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

    @GetMapping("/police/signup")
    public String showPoliceSignupPage() {
        return "police_signup";
    }

    @PostMapping("/send-otp")
    @ResponseBody
    public String sendOtp(@RequestParam String identifier) {
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return "Error: Invalid email format!";
        }
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 digit OTP
        otpStorage.put(identifier, otp);
        System.out.println("[TESTING] Generated OTP for " + identifier + " is: " + otp);

        if (identifier.contains("@")) {
            // Send OTP via Email
            try {
                emailService.sendOtpEmail(identifier, otp);
                return "OTP Sent Successfully to Email!";
            } catch (Exception e) {
                return "Error sending Email: " + e.getMessage();
            }
        } else {
            // Send OTP via Telegram
            try {
                String botToken = "8780573988:AAEAWFDtYg_p-hst4JwqA9RSWN9cNzW7eKk";
                String chatId = "1123697239";
                String text = "🛡️ Guardian Security Code: " + otp + "\nRequested for: " + identifier;
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
    public String registerUser(@RequestParam String identifier, @RequestParam String password, @RequestParam String otp, Model model) {
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Invalid email format!");
            return "signup";
        }
        String storedOtp = otpStorage.get(identifier);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            model.addAttribute("error", "Invalid or expired OTP!");
            return "signup";
        }
        if (userRepository.findByEmail(identifier).isPresent()) {
            model.addAttribute("error", "User already exists!");
            return "signup";
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
        user.setRole("ADMIN");
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
            String role = user.get().getRole();
            if (role == null) role = "ADMIN";
            return "redirect:/dashboard?apiKey=" + user.get().getApiKey() + "&role=" + role;
        }
        model.addAttribute("error", "Invalid Credentials!");
        return "login";
    }

    @PostMapping("/police/signup")
    public String registerPoliceUser(@RequestParam String identifier, @RequestParam String password, @RequestParam(required = false) String otp, Model model) {
        if (identifier.contains("@") && !identifier.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Invalid email format!");
            return "police_signup";
        }
        if (otp != null && !otp.equals(otpStorage.get(identifier))) {
            model.addAttribute("error", "Invalid or expired OTP!");
            return "police_signup";
        }
        if (userRepository.findByEmail(identifier).isPresent()) {
            model.addAttribute("error", "Officer already registered!");
            return "police_signup";
        }
        User user = new User(identifier, password);
        user.setRole("POLICE");
        user.setPhoneNumber(identifier); // just for storing
        userRepository.save(user);
        if (otp != null) otpStorage.remove(identifier);
        return "redirect:/police/login?success";
    }

    @GetMapping("/police/login")
    public String showPoliceLogin() {
        return "police_login";
    }

    @PostMapping("/police/login")
    public String loginPoliceUser(@RequestParam String identifier, @RequestParam String password, Model model) {
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            String role = user.get().getRole();
            if (role == null) role = "ADMIN";
            return "redirect:/dashboard?apiKey=" + user.get().getApiKey() + "&role=" + role;
        }
        model.addAttribute("error", "Invalid Credentials!");
        return "police_login";
    }

    @GetMapping("/logout")
    public String logoutUser() {
        return "redirect:/";
    }
}
