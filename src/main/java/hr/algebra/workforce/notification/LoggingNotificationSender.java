package hr.algebra.workforce.notification;

import hr.algebra.workforce.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationEvent event) {
        log.info("Obavijest za {}: {} — {}", event.recipientEmail(), event.subject(), event.message());
    }
}
