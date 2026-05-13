package com.guardian;

import com.guardian.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// NOTE: This is just a utility class - NOT a SpringBootApplication
// It no longer auto-runs on startup
@Configuration
public class CheckUsers {

    // This bean is disabled - it was causing System.exit(0) on startup
    // @Bean
    public CommandLineRunner run(UserRepository repo) {
        return args -> {
            System.out.println("📊 CURRENT USERS IN DB: " + repo.count());
            repo.findAll().forEach(u -> System.out.println("👤 User: " + u.getEmail()));
        };
    }
}
