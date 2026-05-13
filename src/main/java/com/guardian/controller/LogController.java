package com.guardian.controller;

import com.guardian.entity.LogEntry;
import com.guardian.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LogController {

    @Autowired
    private SecurityService securityService;

    @GetMapping("/logs")
    public List<LogEntry> getLogs() {
        return securityService.getAllLogs();
    }

    @GetMapping("/status")
    public String getStatus() {
        return "🛡️ Cyber Guardian Spring Boot Server is ONLINE and Protecting!";
    }
}
