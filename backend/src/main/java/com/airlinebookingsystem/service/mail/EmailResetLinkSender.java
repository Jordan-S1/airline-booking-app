package com.airlinebookingsystem.service.mail;

import com.airlinebookingsystem.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

/**
 * Emails the reset link over SMTP.
 *
 * <p>Runs on a background thread, for two reasons. An SMTP handshake is slow
 * enough to hold an HTTP worker for seconds if the server is unresponsive. More
 * importantly it closes a timing side channel: a request for a registered
 * address would otherwise take a full round trip longer than one for an
 * unregistered address, which is a measurable way to discover who has an
 * account — the leak {@code PasswordResetService} deliberately avoids in every
 * other respect.
 *
 * <p>Failures are logged and swallowed. By the time this runs the caller has
 * already had its answer, so there is nothing left to fail.
 */
@RequiredArgsConstructor
@Slf4j
public class EmailResetLinkSender implements ResetLinkSender {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String from;

    @Override
    @Async
    public void send(User user, String token, int ttlMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Reset your SkyAir password");
        message.setText(body(user, token, ttlMinutes));

        try {
            mailSender.send(message);
            // The address is logged, the token is not: this line outlives the
            // token by a long way and the two must not share a home.
            log.info("Password reset email sent to {}", user.getEmail());
        } catch (Exception ex) {
            log.warn("Could not send a password reset email to {}: {}",
                    user.getEmail(), ex.getMessage());
        }
    }

    private String body(User user, String token, int ttlMinutes) {
        return """
                Hi %s,

                Someone asked to reset the password on your SkyAir account. Open
                the link below within %d minutes to choose a new one:

                %s/reset-password?token=%s

                The link works once. If you did not ask for this, you can ignore
                this email — nothing has changed and your password still works.

                SkyAir
                """.formatted(user.getFirstName(), ttlMinutes, baseUrl, token);
    }
}
