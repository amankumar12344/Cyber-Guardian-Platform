package com.guardian.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("🛡️ Guardian Security Code");
        message.setText("Welcome to Cyber Guardian!\n\n" +
                "Your verification code is: " + otp + "\n\n" +
                "This code will expire in 5 minutes. If you didn't request this, please ignore this email.");
        mailSender.send(message);
    }
}
