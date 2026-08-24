package com.airlinebookingsystem.service.mail;

import com.airlinebookingsystem.entity.User;

/**
 * Delivers a password reset link.
 *
 * <p>Exists so {@code PasswordResetService} owns the security properties —
 * entropy, hashing, single use, expiry — and nothing else owns delivery. Which
 * implementation is in play is decided once, at startup, by whether SMTP is
 * configured.
 *
 * <p>Implementations must not throw. A link that cannot be delivered is a
 * delivery failure, not a failed request: the token is already persisted, the
 * caller has already been told "if that address has an account, a link has been
 * issued", and that answer stays true.
 */
public interface ResetLinkSender {

    /**
     * @param token the plaintext token — the only copy in existence, since the
     *              database holds just its hash
     */
    void send(User user, String token, int ttlMinutes);
}
