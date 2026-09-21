package hr.algebra.workforce.notification;

import hr.algebra.workforce.event.NotificationEvent;

public interface NotificationSender {

    void send(NotificationEvent event);
}
