package com.airlinebookingsystem.controller;

import com.airlinebookingsystem.dto.auth.AuthResponse;
import com.airlinebookingsystem.dto.auth.LoginRequest;
import com.airlinebookingsystem.config.PasswordEncoderConfig;
import com.airlinebookingsystem.config.SecurityConfig;
import com.airlinebookingsystem.dto.auth.RegisterRequest;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.security.JwtService;
import com.airlinebookingsystem.security.RestAuthenticationEntryPoint;
import com.airlinebookingsystem.service.AuthService;
import com.airlinebookingsystem.service.PasswordResetService;
import com.airlinebookingsystem.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for AuthController.
 *
 * The application's real SecurityConfig is imported rather than relying on
 * Boot's test defaults, which secure everything: without it these requests
 * would be rejected before reaching the controller and the tests would be
 * asserting against the wrong rules. Importing it also means the tests prove
 * /auth/** really is public — every request here is unauthenticated.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, RestAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    // AuthController gained the reset endpoints, so the slice needs this too.
    @MockitoBean private PasswordResetService passwordResetService;

    // The imported chain installs JwtAuthFilter, so its collaborators have to
    // exist. No request here carries a token, so the filter just passes through.
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserService userService;

    private static final String EMAIL = "jordan@example.com";
    private static final String PASSWORD = "PlainPassword1!";

    private RegisterRequest validRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jordan");
        request.setLastName("Test");
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setPhoneNumber("+353871234567");
        return request;
    }

    private LoginRequest validLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .token("signed.jwt.token")
                .email(EMAIL)
                .firstName("Jordan")
                .lastName("Test")
                .role("CUSTOMER")
                .userId(7L)
                .preferredCurrency("EUR")
                .build();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 with the token and user details")
        void registersSuccessfully() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType("application/json")
                            .content(json(validRegisterRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.userId").value(7));
        }

        @Test
        @DisplayName("never returns the password in the response body")
        void doesNotLeakPassword() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType("application/json")
                            .content(json(validRegisterRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("returns 409 when the email is already registered")
        void duplicateEmailIsConflict() throws Exception {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new DuplicateResourceException("Email", EMAIL));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType("application/json")
                            .content(json(validRegisterRequest())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("returns 400 and does not call the service when the email is malformed")
        void rejectsInvalidEmail() throws Exception {
            RegisterRequest request = validRegisterRequest();
            request.setEmail("not-an-email");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType("application/json")
                            .content(json(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("returns 400 when the password is shorter than eight characters")
        void rejectsShortPassword() throws Exception {
            RegisterRequest request = validRegisterRequest();
            request.setPassword("short");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType("application/json")
                            .content(json(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("returns 400 when a required name is blank")
        void rejectsBlankFirstName() throws Exception {
            RegisterRequest request = validRegisterRequest();
            request.setFirstName("   ");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType("application/json")
                            .content(json(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).register(any(RegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with a token for valid credentials")
        void logsInSuccessfully() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content(json(validLoginRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }

        @Test
        @DisplayName("returns 401 with a non-specific message for bad credentials")
        void badCredentialsAreUnauthorized() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content(json(validLoginRequest())))
                    .andExpect(status().isUnauthorized())
                    // The message must not say which of the two was wrong.
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("returns 400 when the body is not valid JSON")
        void rejectsMalformedBody() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content("{\"email\": "))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).login(any(LoginRequest.class));
        }
    }
}
