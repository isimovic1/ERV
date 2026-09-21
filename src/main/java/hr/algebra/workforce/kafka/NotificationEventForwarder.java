package hr.algebra.workforce.kafka;

import hr.algebra.workforce.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventForwarder {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${app.kafka.notification-topic}")
    private String topic;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void forward(NotificationEvent event) {
        try {
            kafkaTemplate.send(topic, String.valueOf(event.employeeId()), event)
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.warn("Obavijest {} nije objavljena na Kafku: {}", event.type(),
                                    throwable.getMessage());
                        } else {
                            log.debug("Obavijest {} objavljena na temu {}", event.type(), topic);
                        }
                    });
        } catch (Exception exception) {
            log.warn("Kafka nije dostupna, obavijest {} je preskočena: {}", event.type(), exception.getMessage());
        }
    }
}
