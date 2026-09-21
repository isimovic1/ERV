package hr.algebra.workforce.event;

import java.time.LocalDateTime;

public record NotificationEvent(NotificationType type,
                                Long employeeId,
                                String employeeName,
                                String recipientEmail,
                                String subject,
                                String message,
                                LocalDateTime occurredAt) {

    public static NotificationEvent of(NotificationType type, Long employeeId, String employeeName,
                                       String recipientEmail, String subject, String message) {
        return new NotificationEvent(type, employeeId, employeeName, recipientEmail, subject, message,
                LocalDateTime.now());
    }
}
