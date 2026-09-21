package hr.algebra.workforce.repository;

import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.LeaveRequest;
import hr.algebra.workforce.model.LeaveType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    @EntityGraph(attributePaths = "user")
    List<LeaveRequest> findByUserIdOrderByStartDateDesc(Long userId);

    List<LeaveRequest> findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long userId, RequestStatus status, LocalDate onOrBefore, LocalDate onOrAfter);

    List<LeaveRequest> findByUserIdAndTypeAndStatusAndStartDateBetweenOrderByStartDateAsc(
            Long userId, LeaveType type, RequestStatus status, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "user")
    List<LeaveRequest> findByUserManagerIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long managerId, RequestStatus status, LocalDate onOrBefore, LocalDate onOrAfter);

    @EntityGraph(attributePaths = "user")
    List<LeaveRequest> findByUserIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            List<Long> userIds, LocalDate onOrBefore, LocalDate onOrAfter);

    @EntityGraph(attributePaths = "user")
    List<LeaveRequest> findByUserManagerIdAndStatusOrderBySubmittedAtAsc(Long managerId, RequestStatus status);

    @EntityGraph(attributePaths = "user")
    List<LeaveRequest> findByUserManagerIdOrderBySubmittedAtDesc(Long managerId);
}
