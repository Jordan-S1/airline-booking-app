package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.PasswordResetToken;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.repository.PasswordResetTokenRepository;
import com.airlinebookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for password reset by token.
 *
 * <p>These are about the security properties rather than the happy path: that
 * the token is never stored, that an unknown address is indistinguishable from
 * a known one, and that a token cannot be used twice or after it expires.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PasswordResetService service;

    private static final String EMAIL = "jordan@example.com";
    private static final String OLD_PASSWORD = "OriginalPass1!";
    private static final String NEW_PASSWORD = "BrandNewPass2!";

    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder);
        ReflectionTestUtils.setField(service, "ttlMinutes", 30);

        user = User.builder()
                .id(1L)
                .email(EMAIL)
                .firstName("Jordan").lastName("Test")
                .password(passwordEncoder.encode(OLD_PASSWORD))
                .role(User.Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** Captures the token record the service persisted. */
    private PasswordResetToken savedToken() {
        ArgumentCaptor<PasswordResetToken> captor =
                ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("requestReset")
    class RequestReset {

        @Test
        @DisplayName("stores only a hash — never the token itself")
        void storesOnlyAHash() {
            service.requestReset(EMAIL);

            PasswordResetToken saved = savedToken();
            // SHA-256 hex is 64 characters; the token is 43 (256 bits, base64url).
            assertThat(saved.getTokenHash()).hasSize(64).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("retires any outstanding token before issuing a new one")
        void retiresOutstandingTokens() {
            service.requestReset(EMAIL);
            verify(tokenRepository).invalidateOutstanding(eq(1L), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("sets an expiry from the configured lifetime")
        void setsExpiry() {
            LocalDateTime before = LocalDateTime.now();
            service.requestReset(EMAIL);

            PasswordResetToken saved = savedToken();
            assertThat(saved.getExpiresAt())
                    .isAfter(before.plusMinutes(29))
                    .isBefore(before.plusMinutes(31));
        }

        @Test
        @DisplayName("issues a token that is not yet used")
        void tokenStartsUnused() {
            service.requestReset(EMAIL);
            assertThat(savedToken().getUsedAt()).isNull();
        }

        /**
         * An unregistered address must be answered exactly like a registered
         * one, or the endpoint becomes a way to test who has an account.
         */
        @Test
        @DisplayName("an unknown address is silently ignored, not reported")
        void unknownAddressIsSilent() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatCode(() -> service.requestReset("nobody@example.com"))
                    .doesNotThrowAnyException();

            verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("survives a null address without issuing anything")
        void nullAddressIsSafe() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatCode(() -> service.requestReset(null)).doesNotThrowAnyException();
            verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("two requests produce different tokens")
        void tokensAreUnpredictable() {
            service.requestReset(EMAIL);
            String first = savedToken().getTokenHash();

            org.mockito.Mockito.reset(tokenRepository);
            when(tokenRepository.save(any(PasswordResetToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.requestReset(EMAIL);
            assertThat(savedToken().getTokenHash()).isNotEqualTo(first);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        /**
         * Issues a real token through the service, then adjusts the stored
         * record to the state under test.
         *
         * <p>The plaintext is generated inside the service and never returned,
         * so the test cannot compute its hash — the lookup is stubbed to
         * resolve whatever token is presented to this record instead.
         */
        private void issue(LocalDateTime expiresAt, LocalDateTime usedAt) {
            service.requestReset(EMAIL);
            PasswordResetToken saved = savedToken();
            saved.setExpiresAt(expiresAt);
            saved.setUsedAt(usedAt);
            when(tokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(saved));
        }

        @Test
        @DisplayName("sets the new password as a hash and retires the token")
        void resetsThePassword() {
            issue(LocalDateTime.now().plusMinutes(30), null);

            service.resetPassword("any-token", NEW_PASSWORD);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(passwordEncoder.matches(NEW_PASSWORD, saved.getValue().getPassword()))
                    .isTrue();
            assertThat(passwordEncoder.matches(OLD_PASSWORD, saved.getValue().getPassword()))
                    .isFalse();
            // atLeastOnce, not once: the helper above issues a real token, and
            // issuing one also retires outstanding tokens. What matters here is
            // that redeeming retires them too.
            verify(tokenRepository, atLeastOnce())
                    .invalidateOutstanding(anyLong(), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("refuses a token that has already been used")
        void refusesUsedToken() {
            issue(LocalDateTime.now().plusMinutes(30), LocalDateTime.now());

            assertThatThrownBy(() -> service.resetPassword("any-token", NEW_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid or has expired");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("refuses a token that has expired")
        void refusesExpiredToken() {
            issue(LocalDateTime.now().minusMinutes(1), null);

            assertThatThrownBy(() -> service.resetPassword("any-token", NEW_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid or has expired");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("refuses a token that was never issued")
        void refusesUnknownToken() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword("made-up", NEW_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).save(any(User.class));
        }

        /**
         * Unknown, expired and already-used all say the same thing. Telling
         * them apart would only help someone probing tokens.
         */
        @Test
        @DisplayName("does not reveal why a token was rejected")
        void failureReasonIsNotDisclosed() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
            String unknown = catchMessage("made-up");

            issue(LocalDateTime.now().plusMinutes(30), LocalDateTime.now());
            String used = catchMessage("any-token");

            assertThat(unknown).isEqualTo(used);
        }

        private String catchMessage(String token) {
            try {
                service.resetPassword(token, NEW_PASSWORD);
                return "(no exception)";
            } catch (IllegalArgumentException e) {
                return e.getMessage();
            }
        }
    }

    @Nested
    @DisplayName("token redeemability")
    class Redeemability {

        private PasswordResetToken token(LocalDateTime expiresAt, LocalDateTime usedAt) {
            return PasswordResetToken.builder()
                    .expiresAt(expiresAt)
                    .usedAt(usedAt)
                    .build();
        }

        @Test
        @DisplayName("is redeemable while unused and unexpired")
        void redeemable() {
            LocalDateTime now = LocalDateTime.now();
            assertThat(token(now.plusMinutes(5), null).isRedeemable(now)).isTrue();
        }

        @Test
        @DisplayName("is not redeemable once used")
        void notRedeemableOnceUsed() {
            LocalDateTime now = LocalDateTime.now();
            assertThat(token(now.plusMinutes(5), now).isRedeemable(now)).isFalse();
        }

        @Test
        @DisplayName("is not redeemable at or past its expiry")
        void notRedeemableWhenExpired() {
            LocalDateTime now = LocalDateTime.now();
            assertThat(token(now.minusSeconds(1), null).isRedeemable(now)).isFalse();
            assertThat(token(now, null).isRedeemable(now)).isFalse();
        }
    }
}
