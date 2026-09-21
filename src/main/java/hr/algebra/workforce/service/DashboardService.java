package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.DashboardView;
import hr.algebra.workforce.dto.MonthlyWorkSummary;
import hr.algebra.workforce.dto.LeaveBalance;
import hr.algebra.workforce.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkEntryService workEntryService;
    private final LeaveService leaveService;
    private final AbsenceService absenceService;
    private final LeaveApprovalService leaveApprovalService;
    private final HolidayService holidayService;

    @Transactional(readOnly = true)
    public DashboardView forUser(Long userId, String fullName, Role role) {
        YearMonth month = YearMonth.now();
        LocalDate today = LocalDate.now();
        MonthlyWorkSummary summary = workEntryService.monthlySummary(userId, month);
        LeaveBalance balance = leaveService.balance(userId, today.getYear());
        boolean onSickLeave = absenceService.isOnSickLeave(userId, today);
        return new DashboardView(fullName, month, summary.totalHours(), summary.totalOvertimeHours(),
                summary.recordedDays(), balance.annualRemainingDays(), balance.annualUsedDays(), balance.annualReservedDays(),
                onSickLeave, isEntryMissingToday(userId, today, onSickLeave),
                pendingTeamRequests(userId, role));
    }

    private boolean isEntryMissingToday(Long userId, LocalDate today, boolean onSickLeave) {
        if (onSickLeave || !isWorkingDay(today) || absenceService.isOnApprovedVacation(userId, today)) {
            return false;
        }
        return !workEntryService.existsForDate(userId, today, null);
    }

    private boolean isWorkingDay(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidayService.holidaysBetween(date, date).contains(date);
    }

    private int pendingTeamRequests(Long userId, Role role) {
        if (role == Role.EMPLOYEE) {
            return 0;
        }
        return leaveApprovalService.pendingForManager(userId).size();
    }
}
