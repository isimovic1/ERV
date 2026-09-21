package hr.algebra.workforce.service;

import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.repository.SickLeaveRepository;
import hr.algebra.workforce.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final SickLeaveRepository sickLeaveRepository;

    @Transactional(readOnly = true)
    public boolean isOnApprovedVacation(Long userId, LocalDate date) {
        return !leaveRequestRepository
                .findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId, RequestStatus.APPROVED, date, date)
                .isEmpty();
    }

    @Transactional(readOnly = true)
    public boolean hasApprovedVacationBetween(Long userId, LocalDate start, LocalDate end) {
        return leaveRequestRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .filter(request -> request.getStatus() == RequestStatus.APPROVED)
                .anyMatch(request -> !start.isAfter(request.getEndDate()) && !end.isBefore(request.getStartDate()));
    }

    @Transactional(readOnly = true)
    public boolean isOnSickLeave(Long userId, LocalDate date) {
        return sickLeaveRepository.findByUserIdAndStartDateLessThanEqualOrderByStartDateDesc(userId, date)
                .stream()
                .anyMatch(leave -> covers(leave, date));
    }

    private boolean covers(SickLeave leave, LocalDate date) {
        return leave.getEndDate() == null || !date.isAfter(leave.getEndDate());
    }
}
