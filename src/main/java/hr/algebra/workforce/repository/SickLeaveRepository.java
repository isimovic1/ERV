package hr.algebra.workforce.repository;

import hr.algebra.workforce.model.SickLeave;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SickLeaveRepository extends JpaRepository<SickLeave, Long> {

    @EntityGraph(attributePaths = "user")
    List<SickLeave> findByUserIdOrderByStartDateDesc(Long userId);

    List<SickLeave> findByUserIdAndStartDateLessThanEqualOrderByStartDateDesc(Long userId, LocalDate date);

    List<SickLeave> findByUserIdAndStartDateBetweenOrderByStartDateAsc(Long userId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "user")
    List<SickLeave> findByUserIdInAndStartDateLessThanEqual(List<Long> userIds, LocalDate date);

    @EntityGraph(attributePaths = "user")
    List<SickLeave> findByUserManagerIdOrderByStartDateDesc(Long managerId);
}
