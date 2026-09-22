package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.LeaveBalance;
import hr.algebra.workforce.dto.LeaveRequestRow;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.event.NotificationEvent;
import hr.algebra.workforce.event.NotificationType;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.model.LeaveRequest;
import hr.algebra.workforce.repository.UserRepository;
import hr.algebra.workforce.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HolidayService holidayService;

    @Value("${app.leave.paid-days-per-year:7}")
    private int paidLeaveDaysPerYear;

    @Transactional(readOnly = true)
    public int workingDaysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        Set<LocalDate> holidays = holidayService.holidaysBetween(start, end);
        return (int) start.datesUntil(end.plusDays(1))
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SATURDAY)
                .filter(date -> date.getDayOfWeek() != DayOfWeek.SUNDAY)
                .filter(date -> !holidays.contains(date))
                .count();
    }

    @Transactional(readOnly = true)
    public LeaveBalance balance(Long userId, int year) {
        User user = requireUser(userId);
        int annualUsed = countDays(userId, year, LeaveType.ANNUAL_LEAVE, RequestStatus.APPROVED);
        int annualReserved = countDays(userId, year, LeaveType.ANNUAL_LEAVE, RequestStatus.PENDING);
        int annualEntitled = user.getVacationDaysPerYear();
        int paidUsed = countDays(userId, year, LeaveType.PAID_LEAVE, RequestStatus.APPROVED);
        int paidReserved = countDays(userId, year, LeaveType.PAID_LEAVE, RequestStatus.PENDING);
        return new LeaveBalance(year,
                annualEntitled, annualUsed, annualReserved, annualEntitled - annualUsed - annualReserved,
                paidLeaveDaysPerYear, paidUsed, paidReserved, paidLeaveDaysPerYear - paidUsed - paidReserved);
    }

    public int remainingDaysFor(LeaveBalance balance, LeaveType type) {
        return type == LeaveType.PAID_LEAVE ? balance.paidRemainingDays() : balance.annualRemainingDays();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestRow> myRequests(Long userId) {
        return leaveRequestRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(LeaveRequestRow::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean overlapsExistingRequest(Long userId, LocalDate start, LocalDate end) {
        return leaveRequestRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .filter(request -> request.getStatus() == RequestStatus.PENDING
                        || request.getStatus() == RequestStatus.APPROVED)
                .anyMatch(request -> !start.isAfter(request.getEndDate()) && !end.isBefore(request.getStartDate()));
    }

    @Transactional
    public void submit(Long userId, LeaveRequestForm form) {
        LeaveRequest request = new LeaveRequest();
        request.setUser(requireUser(userId));
        request.setStartDate(form.getStartDate());
        request.setEndDate(form.getEndDate());
        request.setWorkingDays(workingDaysBetween(form.getStartDate(), form.getEndDate()));
        request.setNote(form.getNote());
        request.setType(form.getType());
        request.setStatus(RequestStatus.PENDING);
        leaveRequestRepository.save(request);
        publishRequested(request);
        log.debug("Zahtjev za godišnji {} - {} korisnika {}", form.getStartDate(), form.getEndDate(), userId);
    }

    @Transactional
    public void cancel(Long userId, Long requestId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .filter(candidate -> candidate.getStatus() == RequestStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Zahtjev nije moguće otkazati: " + requestId));
        request.setStatus(RequestStatus.CANCELLED);
    }

    private void publishRequested(LeaveRequest request) {
        User manager = request.getUser().getManager();
        if (manager == null) {
            log.warn("Zaposlenik {} nema voditelja, obavijest nije objavljena", request.getUser().getId());
            return;
        }
        String label = request.getType() == LeaveType.PAID_LEAVE ? "plaćeni dopust" : "godišnji odmor";
        eventPublisher.publishEvent(NotificationEvent.of(NotificationType.VACATION_REQUESTED,
                request.getUser().getId(), request.getUser().getFullName(), manager.getEmail(),
                "Novi zahtjev za " + label,
                request.getUser().getFullName() + " traži " + label + " od " + request.getStartDate()
                        + " do " + request.getEndDate()
                        + " (" + CroatianPlural.workingDays(request.getWorkingDays()) + ")."));
    }

    private int countDays(Long userId, int year, LeaveType type, RequestStatus status) {
        return leaveRequestRepository
                .findByUserIdAndTypeAndStatusAndStartDateBetweenOrderByStartDateAsc(userId, type, status,
                        LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                .stream()
                .mapToInt(LeaveRequest::getWorkingDays)
                .sum();
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik ne postoji: " + userId));
    }
}
