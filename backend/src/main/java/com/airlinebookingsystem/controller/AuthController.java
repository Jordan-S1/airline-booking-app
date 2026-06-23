package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.auth.AuthResponse;
import com.airlinebookingsystem.dto.auth.LoginRequest;
import com.airlinebookingsystem.dto.auth.RegisterRequest;
import com.airlinebookingsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints for registration and login.
 * No token required — configured as permitAll in SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Body: { firstName, lastName, email, password, phoneNumber? }
     * Returns 201 with token on success, 409 if email already exists.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /api/v1/auth/login
     * Body: { email, password }
     * Returns 200 with token on success, 401 on bad credentials.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login — {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }
}
