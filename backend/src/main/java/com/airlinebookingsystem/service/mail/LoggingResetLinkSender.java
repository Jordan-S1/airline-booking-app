package com.airlinebookingsystem.service.mail;

import com.airlinebookingsystem.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes the reset link to the server log.
 *
 * <p>Used when no SMTP host is configured, which is how this project ships. The
 * security properties survive — only someone who can already read the server's
 * logs can complete a reset — whereas returning the link in the HTTP response
 * would turn "forgot password" into account takeover for any guessable address.
 *
 * <p>This is the <em>only</em> place a token is ever written down. Once SMTP is
 * configured this bean is not created, so a working deployment never puts a
 * live credential in its logs.
 */
@RequiredArgsConstructor
@Slf4j
public class LoggingResetLinkSender implements ResetLinkSender {

    private final String baseUrl;

    @Override
    public void send(User user, String token, int ttlMinutes) {
        log.info("Password reset link for {}: {}/reset-password?token={} (valid {} minutes)",
                user.getEmail(), baseUrl, token, ttlMinutes);
    }
}
