package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.auth.AuthResponse;
import com.airlinebookingsystem.dto.auth.LoginRequest;
import com.airlinebookingsystem.dto.auth.RegisterRequest;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.repository.UserRepository;
import com.airlinebookingsystem.security.JwtService;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/**
 * Handles user registration and login, returning a signed JWT on success.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthResponse register(RegisterRequest request) {
                log.info("Registering new user: {}", request.getEmail());

                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new DuplicateResourceException("Email", request.getEmail());
                }

                User user = User.builder()
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .phoneNumber(request.getPhoneNumber())
                                .role(User.Role.CUSTOMER)
                                .build();

                user = userRepository.save(Objects.requireNonNull(user));
                log.info("User registered successfully: {}", user.getEmail());

                String token = jwtService.generateToken(
                                Map.of("role", user.getRole().name(), "userId", user.getId()),
                                user);

                return buildAuthResponse(user, token);
        }

        public AuthResponse login(LoginRequest request) {
                log.info("Login attempt for: {}", request.getEmail());

                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                User user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

                String token = jwtService.generateToken(
                                Map.of("role", user.getRole().name(), "userId", user.getId()),
                                user);

                log.info("Login successful: {}", user.getEmail());
                return buildAuthResponse(user, token);
        }

        private AuthResponse buildAuthResponse(User user, String token) {
                return AuthResponse.builder()
                                .token(token)
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .role(user.getRole().name())
                                .userId(user.getId())
                                .build();
        }
}
