package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.auth.AuthResponse;
import com.airlinebookingsystem.dto.auth.LoginRequest;
import com.airlinebookingsystem.dto.auth.RegisterRequest;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.exception.DuplicateResourceException;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.UserRepository;
import com.airlinebookingsystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 *
 * The rules that matter here are security ones: a password must never be
 * stored as it arrives, an existing email must not be re-registered, a new
 * account must not be able to choose its own role, and a failed
 * authentication must not produce a token. All collaborators are mocked,
 * so these hold regardless of how the encoder or JWT library behave.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private static final String EMAIL = "jordan@example.com";
    private static final String RAW_PASSWORD = "PlainPassword1!";
    private static final String HASHED_PASSWORD = "$2a$10$hashedhashedhashed";
    private static final String TOKEN = "signed.jwt.token";

    private User persisted;

    @BeforeEach
    void setUp() {
        persisted = User.builder()
                .id(7L)
                .firstName("Jordan").lastName("Test")
                .email(EMAIL)
                .password(HASHED_PASSWORD)
                .phoneNumber("+353871234567")
                .role(User.Role.CUSTOMER)
                .preferredCurrency("EUR")
                .build();

        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(jwtService.generateToken(anyMap(), any(UserDetails.class))).thenReturn(TOKEN);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jordan");
        request.setLastName("Test");
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        request.setPhoneNumber("+353871234567");
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        return request;
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("persists the user and returns a token with their details")
        void registersNewUser() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(persisted);

            AuthResponse response = authService.register(registerRequest());

            assertThat(response.getToken()).isEqualTo(TOKEN);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getFirstName()).isEqualTo("Jordan");
            assertThat(response.getLastName()).isEqualTo("Test");
            assertThat(response.getUserId()).isEqualTo(7L);
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
            assertThat(response.getPreferredCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("stores the encoded password, never the raw one")
        void hashesPasswordBeforeSaving() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(persisted);

            authService.register(registerRequest());

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());

            assertThat(saved.getValue().getPassword())
                    .isEqualTo(HASHED_PASSWORD)
                    .isNotEqualTo(RAW_PASSWORD);
            verify(passwordEncoder).encode(RAW_PASSWORD);
        }

        @Test
        @DisplayName("always assigns the CUSTOMER role — registration cannot self-elevate")
        void alwaysRegistersAsCustomer() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(persisted);

            authService.register(registerRequest());

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getRole()).isEqualTo(User.Role.CUSTOMER);
        }

        @Test
        @DisplayName("rejects an email that is already registered")
        void rejectsDuplicateEmail() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(EMAIL);
        }

        @Test
        @DisplayName("does not save or issue a token when the email is taken")
        void doesNotPersistOnDuplicate() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest()))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(userRepository, never()).save(any(User.class));
            verify(jwtService, never()).generateToken(anyMap(), any(UserDetails.class));
        }

        @Test
        @DisplayName("signs the token with the role and user id as claims")
        void putsRoleAndUserIdInClaims() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(persisted);

            authService.register(registerRequest());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> claims =
                    ArgumentCaptor.forClass(Map.class);
            verify(jwtService).generateToken(claims.capture(), eq(persisted));

            assertThat(claims.getValue())
                    .containsEntry("role", "CUSTOMER")
                    .containsEntry("userId", 7L);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns a token for valid credentials")
        void logsInSuccessfully() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(EMAIL, RAW_PASSWORD));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persisted));

            AuthResponse response = authService.login(loginRequest());

            assertThat(response.getToken()).isEqualTo(TOKEN);
            assertThat(response.getEmail()).isEqualTo(EMAIL);
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
            assertThat(response.getUserId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("hands the submitted credentials to the authentication manager")
        void delegatesToAuthenticationManager() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(EMAIL, RAW_PASSWORD));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persisted));

            authService.login(loginRequest());

            ArgumentCaptor<Authentication> submitted =
                    ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(submitted.capture());

            assertThat(submitted.getValue().getPrincipal()).isEqualTo(EMAIL);
            assertThat(submitted.getValue().getCredentials()).isEqualTo(RAW_PASSWORD);
        }

        @Test
        @DisplayName("propagates a bad-credentials failure instead of issuing a token")
        void badCredentialsProduceNoToken() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest()))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(anyMap(), any(UserDetails.class));
        }

        @Test
        @DisplayName("authenticates before looking the user up")
        void authenticatesBeforeReadingTheUser() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest()))
                    .isInstanceOf(BadCredentialsException.class);

            // A rejected login must not reveal whether the account exists.
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("throws when authentication passes but the account has since gone")
        void throwsWhenUserDisappearsAfterAuthentication() {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(EMAIL, RAW_PASSWORD));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(EMAIL);
        }
    }
}
