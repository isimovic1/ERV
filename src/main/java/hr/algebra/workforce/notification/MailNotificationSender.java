package hr.algebra.workforce.notification;

import hr.algebra.workforce.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RequiredArgsConstructor
@Slf4j
public class MailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String sender;

    @Override
    public void send(NotificationEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(event.recipientEmail());
        message.setSubject(event.subject());
        message.setText(event.message());
        mailSender.send(message);
        log.info("Poslana e-pošta na {}", event.recipientEmail());
    }
}
