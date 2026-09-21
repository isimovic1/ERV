package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.AbsenceKind;
import hr.algebra.workforce.dto.CalendarDay;
import hr.algebra.workforce.dto.TeamCalendar;
import hr.algebra.workforce.dto.TeamCalendarRow;
import hr.algebra.workforce.model.LeaveRequest;
import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.LeaveRequestRepository;
import hr.algebra.workforce.repository.SickLeaveRepository;
import hr.algebra.workforce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamCalendarService {

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final SickLeaveRepository sickLeaveRepository;
    private final HolidayService holidayService;

    @Transactional(readOnly = true)
    public TeamCalendar monthlyCalendar(Long viewerId, Role viewerRole, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<User> employees = viewerRole == Role.ADMIN
                ? userRepository.findAllByOrderByLastNameAscFirstNameAsc()
                : userRepository.findByManagerIdAndActiveTrueOrderByLastNameAsc(viewerId);
        List<LocalDate> days = from.datesUntil(to.plusDays(1)).toList();
        Set<LocalDate> holidays = holidayService.holidaysBetween(from, to);
        Map<Long, Map<LocalDate, AbsenceKind>> absences = collectAbsences(employees, from, to);
        List<TeamCalendarRow> rows = employees.stream()
                .map(employee -> buildRow(employee, days, holidays, absences))
                .toList();
        return new TeamCalendar(month, days, rows);
    }

    private Map<Long, Map<LocalDate, AbsenceKind>> collectAbsences(List<User> employees, LocalDate from,
                                                                  LocalDate to) {
        List<Long> ids = employees.stream().map(User::getId).toList();
        Map<Long, Map<LocalDate, AbsenceKind>> absences = new HashMap<>();
        if (ids.isEmpty()) {
            return absences;
        }
        leaveRequestRepository.findByUserIdInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(ids, to, from)
                .stream()
                .filter(request -> request.getStatus() == RequestStatus.APPROVED
                        || request.getStatus() == RequestStatus.PENDING)
                .forEach(request -> markRange(absences, request.getUser().getId(), request.getStartDate(),
                        request.getEndDate(), from, to, kindOf(request)));
        sickLeaveRepository.findByUserIdInAndStartDateLessThanEqual(ids, to).stream()
                .filter(leave -> leave.getEndDate() == null || !leave.getEndDate().isBefore(from))
                .forEach(leave -> markRange(absences, leave.getUser().getId(), leave.getStartDate(),
                        endOf(leave, to), from, to, AbsenceKind.SICK_LEAVE));
        return absences;
    }

    private LocalDate endOf(SickLeave leave, LocalDate monthEnd) {
        return leave.getEndDate() == null ? monthEnd : leave.getEndDate();
    }

    private AbsenceKind kindOf(LeaveRequest request) {
        boolean approved = request.getStatus() == RequestStatus.APPROVED;
        if (request.getType() == LeaveType.PAID_LEAVE) {
            return approved ? AbsenceKind.PAID_LEAVE : AbsenceKind.PAID_LEAVE_PENDING;
        }
        return approved ? AbsenceKind.ANNUAL_LEAVE : AbsenceKind.ANNUAL_LEAVE_PENDING;
    }

    private void markRange(Map<Long, Map<LocalDate, AbsenceKind>> absences, Long userId, LocalDate start,
                           LocalDate end, LocalDate from, LocalDate to, AbsenceKind kind) {
        LocalDate effectiveStart = start.isBefore(from) ? from : start;
        LocalDate effectiveEnd = end.isAfter(to) ? to : end;
        if (effectiveEnd.isBefore(effectiveStart)) {
            return;
        }
        Map<LocalDate, AbsenceKind> perUser = absences.computeIfAbsent(userId, key -> new HashMap<>());
        effectiveStart.datesUntil(effectiveEnd.plusDays(1)).forEach(date -> perUser.put(date, kind));
    }

    private TeamCalendarRow buildRow(User employee, List<LocalDate> days, Set<LocalDate> holidays,
                                     Map<Long, Map<LocalDate, AbsenceKind>> absences) {
        Map<LocalDate, AbsenceKind> perUser = absences.getOrDefault(employee.getId(), Map.of());
        List<CalendarDay> cells = days.stream()
                .map(date -> new CalendarDay(date, kindFor(date, holidays, perUser)))
                .toList();
        int absentDays = (int) cells.stream()
                .filter(cell -> isAbsence(cell.kind()))
                .count();
        return new TeamCalendarRow(employee.getId(), employee.getFullName(), cells, absentDays);
    }

    private boolean isAbsence(AbsenceKind kind) {
        return kind != AbsenceKind.WORKDAY && kind != AbsenceKind.WEEKEND && kind != AbsenceKind.HOLIDAY;
    }

    private AbsenceKind kindFor(LocalDate date, Set<LocalDate> holidays, Map<LocalDate, AbsenceKind> perUser) {
        AbsenceKind absence = perUser.get(date);
        if (absence != null) {
            return absence;
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return AbsenceKind.WEEKEND;
        }
        return holidays.contains(date) ? AbsenceKind.HOLIDAY : AbsenceKind.WORKDAY;
    }
}
