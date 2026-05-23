package com.guardian.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "welcome";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
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

    @GetMapping("/police/signup")
    public String policeSignup() {
        return "police_signup";
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
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/portal")
    public String portal() {
        return "portal";
    }
}
