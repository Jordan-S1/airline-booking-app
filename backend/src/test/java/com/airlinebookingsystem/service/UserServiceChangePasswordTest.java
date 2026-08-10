package com.airlinebookingsystem.service;

import com.airlinebookingsystem.dto.user.PasswordChangeRequest;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.exception.ResourceNotFoundException;
import com.airlinebookingsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for changing one's own password.
 *
 * <p>A real BCrypt encoder is used rather than a mock: the point of most of
 * these is that the stored value is a hash of the new password and not the
 * password itself, which a stubbed encoder would assert nothing about.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceChangePasswordTest {

    @Mock private UserRepository userRepository;
    @Mock private CurrencyService currencyService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks private UserService userService;

    private static final String CURRENT = "CurrentPass1!";
    private static final String NEW = "BrandNewPass2!";

    private User user;

    @BeforeEach
    void setUp() {
        // @InjectMocks cannot supply the real encoder, so build the service by hand.
        userService = new UserService(userRepository, currencyService, passwordEncoder);

        user = User.builder()
                .id(1L)
                .email("jordan@example.com")
                .firstName("Jordan").lastName("Test")
                .password(passwordEncoder.encode(CURRENT))
                .role(User.Role.CUSTOMER)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PasswordChangeRequest request(String current, String next) {
        return new PasswordChangeRequest(current, next);
    }

    @Test
    @DisplayName("stores a hash of the new password, never the password itself")
    void storesAHashOfTheNewPassword() {
        userService.changePassword(1L, request(CURRENT, NEW));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        String stored = saved.getValue().getPassword();
        assertThat(stored).isNotEqualTo(NEW);
        assertThat(passwordEncoder.matches(NEW, stored)).isTrue();
    }

    @Test
    @DisplayName("the old password stops working once it is changed")
    void oldPasswordNoLongerMatches() {
        userService.changePassword(1L, request(CURRENT, NEW));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        assertThat(passwordEncoder.matches(CURRENT, saved.getValue().getPassword())).isFalse();
    }

    @Test
    @DisplayName("refuses a wrong current password and leaves the stored one untouched")
    void refusesWrongCurrentPassword() {
        String before = user.getPassword();

        assertThatThrownBy(() -> userService.changePassword(1L, request("NotMyPassword1!", NEW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any(User.class));
        assertThat(user.getPassword()).isEqualTo(before);
    }

    /**
     * A wrong current password must not be reported as an authentication
     * failure: the client treats 401 as an expired session and signs the user
     * out, which would be a bewildering answer to mistyping a form field.
     * IllegalArgumentException is what the global handler turns into 400.
     */
    @Test
    @DisplayName("a wrong current password is a bad request, not an auth failure")
    void wrongPasswordIsNotAnAuthFailure() {
        assertThatThrownBy(() -> userService.changePassword(1L, request("NotMyPassword1!", NEW)))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(org.springframework.security.core.AuthenticationException.class);
    }

    @Test
    @DisplayName("refuses a new password identical to the current one")
    void refusesUnchangedPassword() {
        assertThatThrownBy(() -> userService.changePassword(1L, request(CURRENT, CURRENT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("throws when the user does not exist")
    void throwsForUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(99L, request(CURRENT, NEW)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("changing to a password differing only in case is allowed")
    void caseChangeIsAccepted() {
        assertThatCode(() -> userService.changePassword(1L, request(CURRENT, CURRENT.toUpperCase())))
                .doesNotThrowAnyException();
    }
}
