package com.airlinebookingsystem.config;

import com.airlinebookingsystem.service.mail.EmailResetLinkSender;
import com.airlinebookingsystem.service.mail.LoggingResetLinkSender;
import com.airlinebookingsystem.service.mail.ResetLinkSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Chooses how a reset link reaches its owner.
 *
 * <p>The two conditions are exact opposites, so precisely one bean is created
 * and neither depends on the other being evaluated first. {@code
 * @ConditionalOnMissingBean} would read more naturally but is meant for
 * auto-configuration, where ordering is guaranteed; in a user configuration it
 * is a coin toss.
 *
 * <p>The test is emptiness, not presence. Boot's own mail auto-configuration
 * treats a blank {@code spring.mail.host} as configured and hands back a
 * JavaMailSender aimed at nowhere, which fails at send time rather than
 * startup. Requiring a non-blank host keeps "unset" and "set to nothing"
 * meaning the same thing.
 */
@Configuration
@EnableAsync
@Slf4j
public class ResetLinkSenderConfig {

    @Bean
    @ConditionalOnExpression("!'${spring.mail.host:}'.isBlank()")
    public ResetLinkSender emailResetLinkSender(
            JavaMailSender mailSender,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.mail.from}") String from) {
        log.info("Password reset links will be emailed from {}", from);
        return new EmailResetLinkSender(mailSender, baseUrl, from);
    }

    @Bean
    @ConditionalOnExpression("'${spring.mail.host:}'.isBlank()")
    public ResetLinkSender loggingResetLinkSender(@Value("${app.base-url}") String baseUrl) {
        log.info("No SMTP host configured; password reset links go to the server log");
        return new LoggingResetLinkSender(baseUrl);
    }
}
