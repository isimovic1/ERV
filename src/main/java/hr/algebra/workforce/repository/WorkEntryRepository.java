package hr.algebra.workforce.repository;

import hr.algebra.workforce.model.WorkEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkEntryRepository extends JpaRepository<WorkEntry, Long> {

    List<WorkEntry> findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(Long userId, LocalDate from, LocalDate to);

    Optional<WorkEntry> findByUserIdAndWorkDate(Long userId, LocalDate workDate);

    boolean existsByUserIdAndWorkDate(Long userId, LocalDate workDate);

    List<WorkEntry> findByUserIdInAndWorkDateBetweenOrderByWorkDateAsc(List<Long> userIds, LocalDate from, LocalDate to);
}
