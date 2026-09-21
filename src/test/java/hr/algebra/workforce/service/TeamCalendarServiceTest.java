package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.AbsenceKind;
import hr.algebra.workforce.dto.CalendarDay;
import hr.algebra.workforce.dto.TeamCalendar;
import hr.algebra.workforce.dto.TeamCalendarRow;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class TeamCalendarServiceTest extends AbstractIntegrationTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 11);

    @Autowired
    private TeamCalendarService teamCalendarService;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveApprovalService leaveApprovalService;

    @Autowired
    private SickLeaveService sickLeaveService;

    @Autowired
    private HolidayService holidayService;

    private Long managerId;
    private Long employeeId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        adminId = createUser("admin@test.hr", Role.ADMIN, null).getId();
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        managerId = manager.getId();
        employeeId = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager).getId();
        createUser("tudji@test.hr", Role.EMPLOYEE, createUser("drugi@test.hr", Role.MANAGER, null));
    }

    @Test
    void calendarCoversEveryDayOfMonth() {
        TeamCalendar calendar = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH);

        assertThat(calendar.days()).hasSize(30);
        assertThat(calendar.rows()).singleElement()
                .satisfies(row -> assertThat(row.days()).hasSize(30));
    }

    @Test
    void managerSeesOnlyOwnTeamWhileAdminSeesEveryone() {
        assertThat(teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH).rows()).hasSize(1);
        assertThat(teamCalendarService.monthlyCalendar(adminId, Role.ADMIN, MONTH).rows()).hasSize(5);
    }

    @Test
    void weekendsAndHolidaysAreMarked() {
        holidayService.generateForYear(2026);

        TeamCalendarRow row = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH).rows().getFirst();

        assertThat(kindOn(row, LocalDate.of(2026, 11, 7))).isEqualTo(AbsenceKind.WEEKEND);
        assertThat(kindOn(row, LocalDate.of(2026, 11, 1))).isEqualTo(AbsenceKind.WEEKEND);
        assertThat(kindOn(row, LocalDate.of(2026, 11, 18))).isEqualTo(AbsenceKind.HOLIDAY);
        assertThat(kindOn(row, LocalDate.of(2026, 11, 17))).isEqualTo(AbsenceKind.WORKDAY);
        assertThat(row.absentDays()).isZero();
    }

    @Test
    void pendingAndApprovedLeavesAreDistinguished() {
        submitLeave(LeaveType.ANNUAL_LEAVE, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4));

        TeamCalendarRow pending = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH)
                .rows().getFirst();
        assertThat(kindOn(pending, LocalDate.of(2026, 11, 3))).isEqualTo(AbsenceKind.ANNUAL_LEAVE_PENDING);
        assertThat(pending.absentDays()).isEqualTo(3);

        leaveApprovalService.decide(managerId, leaveService.myRequests(employeeId).getFirst().id(),
                RequestStatus.APPROVED, null);

        TeamCalendarRow approved = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH)
                .rows().getFirst();
        assertThat(kindOn(approved, LocalDate.of(2026, 11, 3))).isEqualTo(AbsenceKind.ANNUAL_LEAVE);
    }

    @Test
    void paidLeaveUsesOwnColour() {
        submitLeave(LeaveType.PAID_LEAVE, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10));

        TeamCalendarRow row = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH).rows().getFirst();

        assertThat(kindOn(row, LocalDate.of(2026, 11, 9))).isEqualTo(AbsenceKind.PAID_LEAVE_PENDING);
        assertThat(kindOn(row, LocalDate.of(2026, 11, 11))).isEqualTo(AbsenceKind.WORKDAY);
    }

    @Test
    void openSickLeaveFillsRestOfMonth() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.of(2026, 9, 10));
        sickLeaveService.report(employeeId, form);

        TeamCalendarRow row = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH).rows().getFirst();

        assertThat(kindOn(row, LocalDate.of(2026, 11, 2))).isEqualTo(AbsenceKind.SICK_LEAVE);
        assertThat(kindOn(row, LocalDate.of(2026, 11, 30))).isEqualTo(AbsenceKind.SICK_LEAVE);
        assertThat(row.absentDays()).isEqualTo(30);
    }

    @Test
    void leaveSpanningMonthBoundaryIsClippedToMonth() {
        submitLeave(LeaveType.ANNUAL_LEAVE, LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 3));

        TeamCalendarRow november = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER, MONTH)
                .rows().getFirst();
        TeamCalendarRow october = teamCalendarService.monthlyCalendar(managerId, Role.MANAGER,
                YearMonth.of(2026, 10)).rows().getFirst();

        assertThat(november.absentDays()).isEqualTo(3);
        assertThat(october.absentDays()).isEqualTo(4);
        assertThat(kindOn(november, LocalDate.of(2026, 11, 4))).isEqualTo(AbsenceKind.WORKDAY);
    }

    private AbsenceKind kindOn(TeamCalendarRow row, LocalDate date) {
        return row.days().stream()
                .filter(day -> day.date().equals(date))
                .map(CalendarDay::kind)
                .findFirst()
                .orElseThrow();
    }

    private void submitLeave(LeaveType type, LocalDate start, LocalDate end) {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setType(type);
        form.setStartDate(start);
        form.setEndDate(end);
        leaveService.submit(employeeId, form);
    }

    private User createUser(String email, Role role, User manager) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName(role.name());
        user.setEmail(email);
        user.setPassword("x");
        user.setRole(role);
        user.setHireDate(LocalDate.of(2025, 1, 1));
        user.setManager(manager);
        return userRepository.save(user);
    }
}
