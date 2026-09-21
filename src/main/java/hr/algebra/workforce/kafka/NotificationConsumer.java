package hr.algebra.workforce.kafka;

import hr.algebra.workforce.event.NotificationEvent;
import hr.algebra.workforce.model.NotificationLog;
import hr.algebra.workforce.notification.NotificationSender;
import hr.algebra.workforce.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationSender notificationSender;
    private final NotificationLogRepository notificationLogRepository;

    @KafkaListener(topics = "${app.kafka.notification-topic}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consume(NotificationEvent event) {
        NotificationLog entry = new NotificationLog();
        entry.setType(event.type());
        entry.setRecipientEmail(event.recipientEmail());
        entry.setSubject(event.subject());
        entry.setMessage(event.message());
        entry.setOccurredAt(event.occurredAt());
        try {
            notificationSender.send(event);
            entry.setDelivered(true);
        } catch (Exception exception) {
            entry.setDelivered(false);
            entry.setFailureReason(shorten(exception.getMessage()));
            log.warn("Slanje obavijesti nije uspjelo: {}", exception.getMessage());
        }
        notificationLogRepository.save(entry);
    }

    private String shorten(String message) {
        if (message == null) {
            return "Nepoznata greška";
        }
        return message.length() > 250 ? message.substring(0, 250) : message;
    }
}
