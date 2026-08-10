package com.airlinebookingsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A single-use, short-lived token permitting one password reset.
 *
 * <p>Only the hash is held. The token itself is generated once, handed to the
 * caller, and never written down — the same reasoning that keeps plaintext
 * passwords out of the users table, since a reset token is a temporary
 * password in everything but name.
 */
@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "user", "tokenHash" })
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 of the token, hex encoded. Never the token itself. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Set the moment the token is redeemed, which is what makes it single use. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Whether this token can still be redeemed.
     *
     * @param now the current instant, passed in so the check is testable
     */
    public boolean isRedeemable(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
