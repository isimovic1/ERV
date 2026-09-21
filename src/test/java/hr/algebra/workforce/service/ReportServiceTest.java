package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.dto.MonthlyReportRow;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.form.WorkEntryForm;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest extends AbstractIntegrationTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 9);

    @Autowired
    private ReportService reportService;

    @Autowired
    private WorkEntryService workEntryService;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveApprovalService leaveApprovalService;

    @Autowired
    private SickLeaveService sickLeaveService;

    private Long adminId;
    private Long managerId;
    private Long employeeId;
    private Long foreignEmployeeId;

    @BeforeEach
    void setUp() {
        adminId = createUser("admin@test.hr", Role.ADMIN, null).getId();
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        managerId = manager.getId();
        User otherManager = createUser("drugi@test.hr", Role.MANAGER, null);
        employeeId = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager).getId();
        foreignEmployeeId = createUser("tudji@test.hr", Role.EMPLOYEE, otherManager).getId();
    }

    @Test
    void managerSeesOnlyOwnTeamWhileAdminSeesEveryone() {
        assertThat(reportService.monthlyReport(managerId, Role.MANAGER, MONTH).rows())
                .extracting(MonthlyReportRow::employeeId)
                .containsExactly(employeeId);
        assertThat(reportService.monthlyReport(adminId, Role.ADMIN, MONTH).rows()).hasSize(5);
    }

    @Test
    void hoursAndOvertimeAreSummedForSelectedMonthOnly() {
        workEntryService.save(employeeId, workEntry(LocalDate.of(2026, 9, 1), "8.00", "1.50"));
        workEntryService.save(employeeId, workEntry(LocalDate.of(2026, 9, 2), "7.50", null));
        workEntryService.save(employeeId, workEntry(LocalDate.of(2026, 8, 31), "8.00", null));

        MonthlyReportRow row = reportService.monthlyReport(managerId, Role.MANAGER, MONTH).rows().getFirst();

        assertThat(row.workedDays()).isEqualTo(2);
        assertThat(row.workedHours()).isEqualByComparingTo("15.50");
        assertThat(row.overtimeHours()).isEqualByComparingTo("1.50");
    }

    @Test
    void approvedVacationIsCountedOnlyForDaysInsideMonth() {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setStartDate(LocalDate.of(2026, 9, 28));
        form.setEndDate(LocalDate.of(2026, 10, 2));
        leaveService.submit(employeeId, form);
        leaveApprovalService.decide(managerId, leaveService.myRequests(employeeId).getFirst().id(),
                RequestStatus.APPROVED, null);

        assertThat(reportService.monthlyReport(managerId, Role.MANAGER, MONTH).rows().getFirst().vacationDays())
                .isEqualTo(3);
        assertThat(reportService.monthlyReport(managerId, Role.MANAGER, YearMonth.of(2026, 10)).rows().getFirst()
                .vacationDays()).isEqualTo(2);
    }

    @Test
    void pendingVacationIsNotCounted() {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setStartDate(LocalDate.of(2026, 9, 7));
        form.setEndDate(LocalDate.of(2026, 9, 11));
        leaveService.submit(employeeId, form);

        assertThat(reportService.monthlyReport(managerId, Role.MANAGER, MONTH).rows().getFirst().vacationDays())
                .isZero();
    }

    @Test
    void openSickLeaveIsCappedAtEndOfMonth() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.of(2026, 9, 28));
        sickLeaveService.report(employeeId, form);

        assertThat(reportService.monthlyReport(managerId, Role.MANAGER, MONTH).rows().getFirst().sickDays())
                .isEqualTo(3);
    }

    @Test
    void totalsAggregateAllRows() {
        workEntryService.save(employeeId, workEntry(LocalDate.of(2026, 9, 1), "8.00", "2.00"));
        workEntryService.save(foreignEmployeeId, workEntry(LocalDate.of(2026, 9, 1), "6.00", null));

        MonthlyReport report = reportService.monthlyReport(adminId, Role.ADMIN, MONTH);

        assertThat(report.totalHours()).isEqualByComparingTo("14.00");
        assertThat(report.totalOvertimeHours()).isEqualByComparingTo("2.00");
    }

    private WorkEntryForm workEntry(LocalDate date, String hours, String overtime) {
        WorkEntryForm form = new WorkEntryForm();
        form.setWorkDate(date);
        form.setHours(new BigDecimal(hours));
        form.setOvertimeHours(overtime == null ? null : new BigDecimal(overtime));
        return form;
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
