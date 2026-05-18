package com.guardian.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    private String apiKey;
    private String telegramBotToken;
    private String telegramChatId;
    private java.time.LocalDateTime lastSeen;

    public User() {}

    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.apiKey = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phone) { this.phoneNumber = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getTelegramBotToken() { return telegramBotToken; }
    public void setTelegramBotToken(String token) { this.telegramBotToken = token; }
    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String id) { this.telegramChatId = id; }
    public java.time.LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(java.time.LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
