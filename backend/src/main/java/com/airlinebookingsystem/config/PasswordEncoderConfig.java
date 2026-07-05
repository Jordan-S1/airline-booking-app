package com.airlinebookingsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Extracts PasswordEncoder into its own @Configuration to break the
 * circular dependency chain:
 * SecurityConfig → UserService → PasswordEncoder → SecurityConfig
 * By moving PasswordEncoder here, both SecurityConfig and UserService
 * can inject it independently with no cycle.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
