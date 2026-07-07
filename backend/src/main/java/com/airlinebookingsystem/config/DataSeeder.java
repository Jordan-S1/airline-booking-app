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
 * Default admin credentials:
 *   Email:    admin@airline.com
 *   Password: Admin@1234

 * IMPORTANT: Change the admin password immediately after first login in production.
 * Set ADMIN_EMAIL and ADMIN_PASSWORD environment variables to override defaults.
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
            log.info("Admin account already exists — skipping seeder");
            return;
        }

        String adminEmail = System.getenv().getOrDefault("ADMIN_EMAIL", "admin@airline.com");
        String adminPassword = System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin@1234");

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
        log.warn("======================================================");
        log.warn("Admin account created: {}", adminEmail);
        log.warn("CHANGE THE DEFAULT PASSWORD IMMEDIATELY IN PRODUCTION");
        log.warn("======================================================");
    }
}
