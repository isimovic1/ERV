package hr.algebra.workforce.configuration;

import hr.algebra.workforce.notification.LoggingNotificationSender;
import hr.algebra.workforce.notification.MailNotificationSender;
import hr.algebra.workforce.notification.NotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class NotificationConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public NotificationSender mailNotificationSender(JavaMailSender mailSender,
                                                     @Value("${app.notification.sender}") String sender) {
        return new MailNotificationSender(mailSender, sender);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationSender.class)
    public NotificationSender loggingNotificationSender() {
        return new LoggingNotificationSender();
    }
}
