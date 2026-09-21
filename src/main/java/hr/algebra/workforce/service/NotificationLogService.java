package hr.algebra.workforce.service;

import hr.algebra.workforce.model.NotificationLog;
import hr.algebra.workforce.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional(readOnly = true)
    public Page<NotificationLog> recentEvents(int page, int size) {
        return notificationLogRepository.findAllByOrderByProcessedAtDesc(PageRequest.of(page, size));
    }
}
