package com.guardian.controller;

import com.guardian.entity.User;
import com.guardian.repository.UserRepository;
import com.guardian.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityService securityService;

    @GetMapping("/dashboard")
    public String showDashboard(@RequestParam String apiKey, Model model) {
        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();

        if (user.isEmpty()) return "redirect:/login";

        model.addAttribute("user", user.get());
        model.addAttribute("logs", securityService.getAllLogs());
        return "dashboard";
    }

    @PostMapping("/save-telegram")
    public String saveTelegram(@RequestParam String apiKey, @RequestParam String botToken, @RequestParam String chatId) {
        Optional<User> user = userRepository.findAll().stream()
                .filter(u -> u.getApiKey().equals(apiKey))
                .findFirst();

        if (user.isPresent()) {
            User u = user.get();
            u.setTelegramBotToken(botToken);
            u.setTelegramChatId(chatId);
            userRepository.save(u);
            return "redirect:/dashboard?apiKey=" + apiKey + "&success";
        }
        return "redirect:/login";
    }

    @GetMapping("/api/status")
    @ResponseBody
    @CrossOrigin
    public java.util.Map<String, Boolean> getStatus() {
        return java.util.Collections.singletonMap("locked", com.guardian.Main.isLocked());
    }

    @PostMapping("/api/control")
    @ResponseBody
    @CrossOrigin
    public String controlSystem(@RequestParam String action) {
        if ("lock".equalsIgnoreCase(action)) {
            com.guardian.Main.setLockedRemote(true);
        } else if ("unlock".equalsIgnoreCase(action)) {
            com.guardian.Main.setLockedRemote(false);
        }
        return "OK";
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

    @GetMapping("/api/screenshots/{filename}")
    @ResponseBody
    @CrossOrigin
    public org.springframework.core.io.Resource getScreenshot(@PathVariable String filename) {
        return new org.springframework.core.io.FileSystemResource("screenshots/" + filename);
    }
}
