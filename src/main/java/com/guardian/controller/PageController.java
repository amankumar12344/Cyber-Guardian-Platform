package com.guardian.controller;

import com.guardian.entity.User;
import com.guardian.entity.LogEntry;
import com.guardian.repository.UserRepository;
import com.guardian.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

@Controller
public class PageController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityService securityService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/")
    public String home() {
        return "welcome";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String identifier, @RequestParam String password, Model model, HttpSession session) {
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            session.setAttribute("user", user.get());
            return "redirect:/dashboard";
        }
        model.addAttribute("error", "Invalid Security Credentials.");
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String handleSignup(@RequestParam String identifier, @RequestParam String password, @RequestParam String otp, Model model, HttpSession session) {
        // Simple mock/redirect since OTP verification is done in AuthController REST endpoint
        // Let's create user if details match
        Optional<User> existing = userRepository.findByEmail(identifier);
        if (existing.isPresent()) {
            model.addAttribute("error", "User already exists!");
            return "signup";
        }
        User user = new User(identifier, passwordEncoder.encode(password));
        user.setRole("ADMIN");
        if (identifier.contains("@")) {
            user.setPhoneNumber(null);
        } else {
            user.setPhoneNumber(identifier);
        }
        userRepository.save(user);
        session.setAttribute("user", user);
        return "redirect:/dashboard";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/police/login")
    public String policeLogin() {
        return "police_login";
    }

    @PostMapping("/police/login")
    public String handlePoliceLogin(@RequestParam String identifier, @RequestParam String password, Model model, HttpSession session) {
        Optional<User> user = userRepository.findByEmail(identifier);
        if (user.isPresent() && "POLICE".equalsIgnoreCase(user.get().getRole()) && passwordEncoder.matches(password, user.get().getPassword())) {
            session.setAttribute("user", user.get());
            return "redirect:/dashboard";
        }
        model.addAttribute("error", "Invalid Officer Credentials.");
        return "police_login";
    }

    @GetMapping("/police/signup")
    public String policeSignup() {
        return "police_signup";
    }

    @PostMapping("/police/signup")
    public String handlePoliceSignup(@RequestParam String identifier, @RequestParam String password, Model model, HttpSession session) {
        Optional<User> existing = userRepository.findByEmail(identifier);
        if (existing.isPresent()) {
            model.addAttribute("error", "Officer already exists!");
            return "police_signup";
        }
        User user = new User(identifier, passwordEncoder.encode(password));
        user.setRole("POLICE");
        userRepository.save(user);
        session.setAttribute("user", user);
        return "redirect:/dashboard";
    }

    @GetMapping("/police/forgot-password")
    public String policeForgotPassword() {
        return "police_forgot_password";
    }

    @GetMapping("/police/reset-password")
    public String policeResetPassword() {
        return "police_reset_password";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("role", user.getRole() != null ? user.getRole().toUpperCase() : "ADMIN");
        
        List<LogEntry> logs = securityService.getLogsByTargetId("ALL");
        model.addAttribute("logs", logs);
        model.addAttribute("targets", securityService.getAllTargetIds());
        model.addAttribute("currentTarget", "ALL");
        
        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("totalBreaches", logs.size());
        model.addAttribute("activeUsers", allUsers.size());
        
        long criticalAlerts = logs.stream()
                .filter(log -> log.getDetails().toLowerCase().contains("kockroach") || 
                               log.getDetails().toLowerCase().contains("spying"))
                .count();
        model.addAttribute("criticalAlerts", criticalAlerts);
        
        List<String> fileList = new ArrayList<>();
        java.io.File rootFolder = new java.io.File("screenshots/");
        if (rootFolder.exists() && rootFolder.isDirectory()) {
            java.io.File[] files = rootFolder.listFiles(f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".png") || f.getName().toLowerCase().endsWith(".mp4")));
            if (files != null) {
                for (java.io.File f : files) {
                    fileList.add(f.getName());
                }
            }
        }
        fileList.sort((a, b) -> b.compareToIgnoreCase(a));
        model.addAttribute("screenshots", fileList);
        
        return "dashboard";
    }

    @GetMapping("/decoy")
    public String decoy() {
        return "decoy";
    }

    @GetMapping("/portal")
    public String portal() {
        return "portal";
    }

    @GetMapping("/3rd-AI-Agent.exe")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadAgent() {
        java.io.File file = new java.io.File("kockroch.exe");
        if (file.exists()) {
            org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"3rd-AI-Agent.exe\"")
                    .contentLength(file.length())
                    .body(resource);
        } else {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .header(org.springframework.http.HttpHeaders.LOCATION, "https://drive.google.com/uc?export=download&id=1PVApeaQzobxZ7UkYYz56MHmI06OojqL7")
                    .build();
        }
    }
}
