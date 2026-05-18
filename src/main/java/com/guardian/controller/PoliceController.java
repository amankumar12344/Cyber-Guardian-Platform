package com.guardian.controller;

import com.guardian.entity.LogEntry;
import com.guardian.entity.User;
import com.guardian.repository.LogRepository;
import com.guardian.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Optional;

import java.util.List;

@Controller
@RequestMapping("/police")
public class PoliceController {

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String showPoliceDashboard(@RequestParam(required = false) String apiKey, @RequestParam(required = false, defaultValue = "ALL") String targetId, Model model) {
        if (apiKey == null || apiKey.isEmpty()) return "redirect:/police/login";
        return "redirect:/dashboard?apiKey=" + apiKey + "&role=POLICE&targetId=" + targetId;
    }
}
