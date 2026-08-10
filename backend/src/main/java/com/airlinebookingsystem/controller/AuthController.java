package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.auth.AuthResponse;
import com.airlinebookingsystem.dto.auth.ForgotPasswordRequest;
import com.airlinebookingsystem.dto.auth.LoginRequest;
import com.airlinebookingsystem.dto.auth.ResetPasswordRequest;
import com.airlinebookingsystem.dto.auth.RegisterRequest;
import com.airlinebookingsystem.service.AuthService;
import com.airlinebookingsystem.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Register and login to receive a JWT token")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Register a new user", description = "Creates a new CUSTOMER account and returns a JWT token immediately")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login", description = "Authenticates an existing user and returns a JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login — {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Request a password reset",
            description = "Always returns 204, whether or not the address is registered — "
                    + "answering differently would let anyone test who has an account. "
                    + "No email is sent in this project: the link is written to the "
                    + "server log.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Request accepted"),
            @ApiResponse(responseCode = "400", description = "Email missing or malformed", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // The address is not logged here — the service logs the outcome instead.
        log.info("POST /auth/forgot-password");
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reset a password using a token",
            description = "Single use, and only until the token expires.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset"),
            @ApiResponse(responseCode = "400", description = "Token unknown, expired or already used", content = @Content)
    })
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // The token is a credential; it is never written to the request log.
        log.info("POST /auth/reset-password");
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
