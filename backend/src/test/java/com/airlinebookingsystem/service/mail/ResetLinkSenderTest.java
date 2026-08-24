package com.airlinebookingsystem.service.mail;

import com.airlinebookingsystem.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Tests for reset link delivery.
 *
 * <p>The token is a credential with a thirty-minute life, so what matters is
 * where it ends up: in the message body, addressed to the account holder, as a
 * link someone can actually click — and nowhere else.
 */
@ExtendWith(MockitoExtension.class)
class ResetLinkSenderTest {

    @Mock private JavaMailSender mailSender;

    private static final String BASE_URL = "https://skyair.example";
    private static final String FROM = "no-reply@skyair.example";
    private static final String TOKEN = "8Kd2_qZr-token";

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("jordan@example.com")
                .firstName("Jordan")
                .lastName("Test")
                .role(User.Role.CUSTOMER)
                .build();
    }

    @Nested
    @DisplayName("email delivery")
    class Email {

        private EmailResetLinkSender sender;

        @BeforeEach
        void setUp() {
            sender = new EmailResetLinkSender(mailSender, BASE_URL, FROM);
        }

        private SimpleMailMessage sent() {
            ArgumentCaptor<SimpleMailMessage> captor =
                    ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            return captor.getValue();
        }

        @Test
        @DisplayName("addresses the account holder, from the configured sender")
        void addressesTheAccountHolder() {
            sender.send(user, TOKEN, 30);

            SimpleMailMessage message = sent();
            assertThat(message.getTo()).containsExactly("jordan@example.com");
            assertThat(message.getFrom()).isEqualTo(FROM);
            assertThat(message.getSubject()).contains("Reset your SkyAir password");
        }

        /**
         * An absolute URL, because a relative path is not clickable from an
         * inbox — the reason app.base-url exists at all.
         */
        @Test
        @DisplayName("carries an absolute, clickable link")
        void carriesAnAbsoluteLink() {
            sender.send(user, TOKEN, 30);

            assertThat(sent().getText())
                    .contains("https://skyair.example/reset-password?token=" + TOKEN);
        }

        @Test
        @DisplayName("states the lifetime it was given, not a hardcoded one")
        void statesTheConfiguredLifetime() {
            sender.send(user, TOKEN, 45);
            assertThat(sent().getText()).contains("45 minutes");
        }

        /**
         * The token is already committed and the caller already answered by the
         * time this runs, so there is nothing left for a throw to accomplish.
         */
        @Test
        @DisplayName("swallows a send failure rather than propagating it")
        void swallowsSendFailures() {
            doThrow(new MailSendException("connection refused"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            assertThatCode(() -> sender.send(user, TOKEN, 30)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("log delivery")
    class Logging {

        /**
         * The fallback when no SMTP host is set. It has to produce the same
         * absolute link, since it is the only route to the token in that mode.
         */
        @Test
        @DisplayName("does not throw, and never touches a mail sender")
        void writesTheLinkWithoutMail() {
            LoggingResetLinkSender sender = new LoggingResetLinkSender(BASE_URL);

            assertThatCode(() -> sender.send(user, TOKEN, 30)).doesNotThrowAnyException();
            org.mockito.Mockito.verifyNoInteractions(mailSender);
        }
    }
}
