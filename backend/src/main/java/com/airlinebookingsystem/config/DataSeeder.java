package com.airlinebookingsystem.config;

import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds an initial admin account on first startup if none exists.
 * Requires two environment variables to be set:
 *   ADMIN_EMAIL — the admin account email
 *   ADMIN_PASSWORD — the admin account password (min 8 chars recommended)
 * The app will refuse to start if either variable is missing.
 * Set these in your .env file for local dev and in your platform's
 * secret manager for production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        // Only seed if no admin exists
        if (!userRepository.findByRole(User.Role.ADMIN).isEmpty()) {
            log.info("Admin account already exists - skipping seeder");
            return;
        }

        // Fail loudly if env vars are not set — no fallback defaults
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL environment variable is not set. " +
                            "Add it to your .env file and restart the application.");
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD environment variable is not set. " +
                            "Add it to your .env file and restart the application.");
        }

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        userRepository.save(admin);
        log.info("Admin account created: {}", adminEmail);
    }
}
