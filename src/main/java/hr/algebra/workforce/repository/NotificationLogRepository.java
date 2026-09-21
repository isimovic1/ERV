package hr.algebra.workforce.repository;

import hr.algebra.workforce.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findAllByOrderByProcessedAtDesc(Pageable pageable);
}
