package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.dto.MonthlyReportRow;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.model.WorkEntry;
import hr.algebra.workforce.repository.SickLeaveRepository;
import hr.algebra.workforce.repository.UserRepository;
import hr.algebra.workforce.repository.LeaveRequestRepository;
import hr.algebra.workforce.repository.WorkEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final WorkEntryRepository workEntryRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final LeaveService leaveService;

    @Transactional(readOnly = true)
    public MonthlyReport monthlyReport(Long viewerId, Role viewerRole, YearMonth month) {
        List<User> employees = viewerRole == Role.ADMIN
                ? userRepository.findAllByOrderByLastNameAscFirstNameAsc()
                : userRepository.findByManagerIdAndActiveTrueOrderByLastNameAsc(viewerId);
        List<MonthlyReportRow> rows = employees.stream()
                .map(employee -> buildRow(employee, month))
                .toList();
        return MonthlyReport.of(month, rows);
    }

    private MonthlyReportRow buildRow(User employee, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<WorkEntry> entries = workEntryRepository
                .findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(employee.getId(), from, to);
        return new MonthlyReportRow(employee.getId(), employee.getFullName(),
                sum(entries, WorkEntry::getHours), sum(entries, WorkEntry::getOvertimeHours), entries.size(),
                vacationDays(employee.getId(), from, to), sickDays(employee.getId(), from, to));
    }

    private int vacationDays(Long userId, LocalDate from, LocalDate to) {
        return leaveRequestRepository
                .findByUserIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId, RequestStatus.APPROVED, to, from)
                .stream()
                .mapToInt(request -> overlapWorkingDays(request.getStartDate(), request.getEndDate(), from, to))
                .sum();
    }

    private int sickDays(Long userId, LocalDate from, LocalDate to) {
        return sickLeaveRepository.findByUserIdAndStartDateLessThanEqualOrderByStartDateDesc(userId, to).stream()
                .filter(leave -> leave.getEndDate() == null || !leave.getEndDate().isBefore(from))
                .mapToInt(leave -> overlapWorkingDays(leave.getStartDate(), endOf(leave, to), from, to))
                .sum();
    }

    private LocalDate endOf(SickLeave leave, LocalDate monthEnd) {
        return leave.getEndDate() == null ? monthEnd : leave.getEndDate();
    }

    private int overlapWorkingDays(LocalDate start, LocalDate end, LocalDate from, LocalDate to) {
        LocalDate effectiveStart = start.isBefore(from) ? from : start;
        LocalDate effectiveEnd = end.isAfter(to) ? to : end;
        return leaveService.workingDaysBetween(effectiveStart, effectiveEnd);
    }

    private BigDecimal sum(List<WorkEntry> entries, Function<WorkEntry, BigDecimal> extractor) {
        return entries.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
