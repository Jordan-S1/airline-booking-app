package com.airlinebookingsystem.service;

import com.airlinebookingsystem.entity.PasswordResetToken;
import com.airlinebookingsystem.entity.User;
import com.airlinebookingsystem.repository.PasswordResetTokenRepository;
import com.airlinebookingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Password reset by single-use token.
 *
 * <p><strong>No email is sent.</strong> This project has no mail provider, so
 * the generated link is written to the server log and nowhere else. That keeps
 * the security properties intact — only someone who can already read the
 * server's logs can complete a reset — whereas returning the token in the HTTP
 * response would turn "forgot password" into an account takeover for any
 * address an attacker can guess. Wiring in a mail provider means changing
 * where the link is delivered and nothing else.
 *
 * <p>The design points that matter:
 * <ul>
 *   <li>The token is 256 bits from {@link SecureRandom}, not a UUID.</li>
 *   <li>Only its SHA-256 is stored, so this table is useless if it leaks.</li>
 *   <li>Issuing a token invalidates the account's outstanding ones.</li>
 *   <li>Requesting a reset answers identically whether or not the address is
 *       registered, so the endpoint cannot be used to enumerate users.</li>
 *   <li>Redeeming a token invalidates it before the password is written.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password-reset.ttl-minutes:30}")
    private int ttlMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Issues a reset token for the address, if it belongs to an account.
     *
     * <p>Returns nothing either way. An address that is not registered is not
     * an error the caller is told about: answering differently would let
     * anyone test whether a given person has an account here.
     */
    public void requestReset(String email) {
        Optional<User> found = userRepository.findByEmail(email == null ? "" : email.trim());

        if (found.isEmpty()) {
            // Logged, because it is worth seeing in aggregate, but the caller
            // gets the same reply as a hit.
            log.info("Password reset requested for an address with no account");
            return;
        }

        User user = found.get();
        LocalDateTime now = LocalDateTime.now();

        // One live token per account: a second request retires the first, so a
        // link sitting in an old message cannot still work.
        tokenRepository.invalidateOutstanding(user.getId(), now);

        String token = generateToken();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(token))
                .expiresAt(now.plusMinutes(ttlMinutes))
                .createdAt(now)
                .build());

        // Stands in for the email. The token appears here once and is not
        // stored, so this log line is the only copy in existence.
        log.info("Password reset link for {}: /reset-password?token={} (valid {} minutes)",
                user.getEmail(), token, ttlMinutes);
    }

    /**
     * Redeems a token and sets the new password.
     *
     * @throws IllegalArgumentException if the token is unknown, expired or
     *                                  already used — all reported the same way,
     *                                  since telling them apart only helps
     *                                  someone guessing
     */
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken record = tokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException(
                        "This reset link is invalid or has expired. Request a new one."));

        if (!record.isRedeemable(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "This reset link is invalid or has expired. Request a new one.");
        }

        User user = record.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Retire every token for the account, not just this one: if two were
        // somehow live, using one must not leave the other usable.
        tokenRepository.invalidateOutstanding(user.getId(), LocalDateTime.now());

        log.info("Password reset completed for user ID: {}", user.getId());
    }

    /** 256 bits of entropy, URL-safe. A UUID is not a credential. */
    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, not BCrypt.
     *
     * <p>BCrypt is deliberately slow to make guessing a low-entropy human
     * password expensive. This value is 256 random bits, so guessing is not a
     * threat, and a slow hash would only be a way to make every lookup slow.
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
