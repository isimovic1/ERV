package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.MonthlyWorkSummary;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.WorkEntryForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkEntryServiceTest extends AbstractIntegrationTest {

    @Autowired
    private WorkEntryService workEntryService;



    private Long employeeId;
    private Long otherEmployeeId;

    @BeforeEach
    void setUp() {
        employeeId = createEmployee("prvi@test.hr").getId();
        otherEmployeeId = createEmployee("drugi@test.hr").getId();
    }

    @Test
    void saveStoresEntryAndCountsMonthlyTotals() {
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 1), "8.00", "1.50"));
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 2), "7.50", null));

        MonthlyWorkSummary summary = workEntryService.monthlySummary(employeeId, YearMonth.of(2026, 9));

        assertThat(summary.recordedDays()).isEqualTo(2);
        assertThat(summary.totalHours()).isEqualByComparingTo("15.50");
        assertThat(summary.totalOvertimeHours()).isEqualByComparingTo("1.50");
    }

    @Test
    void monthlySummaryIgnoresOtherMonths() {
        workEntryService.save(employeeId, form(LocalDate.of(2026, 8, 31), "8.00", null));
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 1), "8.00", null));

        assertThat(workEntryService.monthlySummary(employeeId, YearMonth.of(2026, 9)).recordedDays()).isEqualTo(1);
    }

    @Test
    void existsForDateDetectsDuplicateAndIgnoresEditedEntry() {
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 3), "8.00", null));
        Long entryId = firstEntryId();

        assertThat(workEntryService.existsForDate(employeeId, LocalDate.of(2026, 9, 3), null)).isTrue();
        assertThat(workEntryService.existsForDate(employeeId, LocalDate.of(2026, 9, 3), entryId)).isFalse();
        assertThat(workEntryService.existsForDate(otherEmployeeId, LocalDate.of(2026, 9, 3), null)).isFalse();
    }

    @Test
    void entryOfAnotherEmployeeIsNotAccessible() {
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 4), "8.00", null));
        Long entryId = firstEntryId();

        assertThatThrownBy(() -> workEntryService.findForEdit(otherEmployeeId, entryId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> workEntryService.delete(otherEmployeeId, entryId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveWithIdUpdatesExistingEntry() {
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 5), "8.00", null));
        WorkEntryForm stored = workEntryService.findForEdit(employeeId, firstEntryId());
        stored.setHours(new BigDecimal("6.00"));

        workEntryService.save(employeeId, stored);

        MonthlyWorkSummary summary = workEntryService.monthlySummary(employeeId, YearMonth.of(2026, 9));
        assertThat(summary.recordedDays()).isEqualTo(1);
        assertThat(summary.totalHours()).isEqualByComparingTo("6.00");
    }

    @Test
    void missingOvertimeIsStoredAsZero() {
        workEntryService.save(employeeId, form(LocalDate.of(2026, 9, 6), "8.00", null));

        assertThat(workEntryService.monthlySummary(employeeId, YearMonth.of(2026, 9))
                .entries().getFirst().overtimeHours()).isEqualByComparingTo("0");
    }

    private Long firstEntryId() {
        return workEntryService.monthlySummary(employeeId, YearMonth.of(2026, 9)).entries().getFirst().id();
    }

    private WorkEntryForm form(LocalDate date, String hours, String overtime) {
        WorkEntryForm form = new WorkEntryForm();
        form.setWorkDate(date);
        form.setHours(new BigDecimal(hours));
        form.setOvertimeHours(overtime == null ? null : new BigDecimal(overtime));
        return form;
    }

    private User createEmployee(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Korisnik");
        user.setEmail(email);
        user.setPassword("x");
        user.setRole(Role.EMPLOYEE);
        user.setHireDate(LocalDate.of(2025, 1, 1));
        return userRepository.save(user);
    }
}
