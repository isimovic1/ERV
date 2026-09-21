package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.SickLeaveRow;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.SickLeaveCloseForm;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.SickLeaveStatus;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SickLeaveServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SickLeaveService sickLeaveService;

    @Autowired
    private SickLeaveReviewService sickLeaveReviewService;

    @Autowired
    private AbsenceService absenceService;

    private Long managerId;
    private Long employeeId;
    private Long otherEmployeeId;
    private Long unrelatedManagerId;

    @BeforeEach
    void setUp() {
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        managerId = manager.getId();
        unrelatedManagerId = createUser("drugi.voditelj@test.hr", Role.MANAGER, null).getId();
        employeeId = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager).getId();
        otherEmployeeId = createUser("kolega@test.hr", Role.EMPLOYEE, manager).getId();
    }

    @Test
    void openLeaveIsReportedAndClosedLeaveIsStoredAsClosed() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), null));
        sickLeaveService.report(otherEmployeeId, form(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 16)));

        assertThat(firstLeave(employeeId).status()).isEqualTo(SickLeaveStatus.REPORTED);
        assertThat(firstLeave(otherEmployeeId).status()).isEqualTo(SickLeaveStatus.CLOSED);
    }

    @Test
    void openLeaveCoversEveryLaterDate() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), null));

        assertThat(absenceService.isOnSickLeave(employeeId, LocalDate.of(2026, 9, 13))).isFalse();
        assertThat(absenceService.isOnSickLeave(employeeId, LocalDate.of(2026, 9, 14))).isTrue();
        assertThat(absenceService.isOnSickLeave(employeeId, LocalDate.of(2026, 12, 31))).isTrue();
    }

    @Test
    void closedLeaveCoversOnlyItsOwnRange() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 16)));

        assertThat(absenceService.isOnSickLeave(employeeId, LocalDate.of(2026, 9, 16))).isTrue();
        assertThat(absenceService.isOnSickLeave(employeeId, LocalDate.of(2026, 9, 17))).isFalse();
    }

    @Test
    void overlapIsDetectedAgainstOpenLeave() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), null));

        assertThat(sickLeaveService.hasOverlappingLeave(employeeId, LocalDate.of(2026, 10, 1), null)).isTrue();
        assertThat(sickLeaveService.hasOverlappingLeave(employeeId, LocalDate.of(2026, 9, 13),
                LocalDate.of(2026, 9, 13))).isFalse();
        assertThat(sickLeaveService.hasOverlappingLeave(otherEmployeeId, LocalDate.of(2026, 9, 14), null)).isFalse();
    }

    @Test
    void closeSetsEndDateAndStatus() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), null));
        Long leaveId = firstLeave(employeeId).id();

        sickLeaveService.close(employeeId, leaveId, closeForm(LocalDate.of(2026, 9, 16)));

        SickLeaveRow closed = firstLeave(employeeId);
        assertThat(closed.status()).isEqualTo(SickLeaveStatus.CLOSED);
        assertThat(closed.endDate()).isEqualTo(LocalDate.of(2026, 9, 16));
        assertThat(absenceService.isOnSickLeave(employeeId, LocalDate.of(2026, 9, 17))).isFalse();
    }

    @Test
    void closeRejectsForeignAlreadyClosedAndInvalidDates() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), null));
        Long leaveId = firstLeave(employeeId).id();

        assertThatThrownBy(() -> sickLeaveService.close(otherEmployeeId, leaveId, closeForm(LocalDate.of(2026, 9, 16))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> sickLeaveService.close(employeeId, leaveId, closeForm(LocalDate.of(2026, 9, 10))))
                .isInstanceOf(IllegalArgumentException.class);

        sickLeaveService.close(employeeId, leaveId, closeForm(LocalDate.of(2026, 9, 16)));
        assertThatThrownBy(() -> sickLeaveService.close(employeeId, leaveId, closeForm(LocalDate.of(2026, 9, 17))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onlyOwnManagerCanConfirmReportedLeave() {
        sickLeaveService.report(employeeId, form(LocalDate.of(2026, 9, 14), null));
        Long leaveId = firstLeave(employeeId).id();

        assertThat(sickLeaveReviewService.teamLeaves(unrelatedManagerId)).isEmpty();
        assertThat(sickLeaveReviewService.teamLeaves(managerId)).hasSize(1);
        assertThatThrownBy(() -> sickLeaveReviewService.confirm(unrelatedManagerId, leaveId))
                .isInstanceOf(ResourceNotFoundException.class);

        sickLeaveReviewService.confirm(managerId, leaveId);
        assertThat(firstLeave(employeeId).status()).isEqualTo(SickLeaveStatus.CONFIRMED);
        assertThatThrownBy(() -> sickLeaveReviewService.confirm(managerId, leaveId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private SickLeaveRow firstLeave(Long userId) {
        return sickLeaveService.myLeaves(userId).getFirst();
    }

    private SickLeaveForm form(LocalDate start, LocalDate end) {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(start);
        form.setEndDate(end);
        form.setDocumentReference("DZ-1");
        return form;
    }

    private SickLeaveCloseForm closeForm(LocalDate end) {
        SickLeaveCloseForm form = new SickLeaveCloseForm();
        form.setEndDate(end);
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
