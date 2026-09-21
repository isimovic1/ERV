package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.LeaveBalance;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaveServiceTest extends AbstractIntegrationTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveApprovalService leaveApprovalService;



    private Long managerId;
    private Long employeeId;
    private Long unrelatedManagerId;

    @BeforeEach
    void setUp() {
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        managerId = manager.getId();
        unrelatedManagerId = createUser("drugi.voditelj@test.hr", Role.MANAGER, null).getId();
        employeeId = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager).getId();
    }

    @Test
    void workingDaysSkipWeekends() {
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)))
                .isEqualTo(5);
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)))
                .isEqualTo(6);
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11)))
                .isZero();
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 5)))
                .isZero();
    }

    @Test
    void pendingRequestReservesDaysAndApprovalConsumesThem() {
        submit(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));

        LeaveBalance reserved = leaveService.balance(employeeId, 2026);
        assertThat(reserved.annualReservedDays()).isEqualTo(5);
        assertThat(reserved.annualUsedDays()).isZero();
        assertThat(reserved.annualRemainingDays()).isEqualTo(15);

        leaveApprovalService.decide(managerId, firstRequestId(), RequestStatus.APPROVED, "U redu");

        LeaveBalance approved = leaveService.balance(employeeId, 2026);
        assertThat(approved.annualUsedDays()).isEqualTo(5);
        assertThat(approved.annualReservedDays()).isZero();
        assertThat(approved.annualRemainingDays()).isEqualTo(15);
    }

    @Test
    void rejectedRequestReturnsDaysToBalance() {
        submit(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
        leaveApprovalService.decide(managerId, firstRequestId(), RequestStatus.REJECTED, "Nije moguće");

        LeaveBalance balance = leaveService.balance(employeeId, 2026);
        assertThat(balance.annualRemainingDays()).isEqualTo(20);
        assertThat(balance.annualUsedDays()).isZero();
        assertThat(balance.annualReservedDays()).isZero();
    }

    @Test
    void overlapIsDetectedOnlyForActiveRequests() {
        submit(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));

        assertThat(leaveService.overlapsExistingRequest(employeeId,
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 13))).isTrue();
        assertThat(leaveService.overlapsExistingRequest(employeeId,
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16))).isFalse();

        leaveService.cancel(employeeId, firstRequestId());
        assertThat(leaveService.overlapsExistingRequest(employeeId,
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9))).isFalse();
    }

    @Test
    void onlyOwnPendingRequestCanBeCancelled() {
        submit(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
        Long requestId = firstRequestId();

        assertThatThrownBy(() -> leaveService.cancel(managerId, requestId))
                .isInstanceOf(ResourceNotFoundException.class);

        leaveApprovalService.decide(managerId, requestId, RequestStatus.APPROVED, null);
        assertThatThrownBy(() -> leaveService.cancel(employeeId, requestId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void foreignManagerCannotDecide() {
        submit(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
        Long requestId = firstRequestId();

        assertThatThrownBy(() -> leaveApprovalService.decide(unrelatedManagerId, requestId,
                RequestStatus.APPROVED, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(leaveApprovalService.pendingForManager(unrelatedManagerId)).isEmpty();
        assertThat(leaveApprovalService.pendingForManager(managerId)).hasSize(1);
    }

    @Test
    void decidedRequestCannotBeDecidedAgain() {
        submit(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
        Long requestId = firstRequestId();
        leaveApprovalService.decide(managerId, requestId, RequestStatus.APPROVED, null);

        assertThatThrownBy(() -> leaveApprovalService.decide(managerId, requestId,
                RequestStatus.REJECTED, null)).isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void paidLeaveHasSeparateQuotaFromAnnualLeave() {
        submitPaidLeave(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7));

        LeaveBalance balance = leaveService.balance(employeeId, 2026);
        assertThat(balance.paidReservedDays()).isEqualTo(3);
        assertThat(balance.paidRemainingDays()).isEqualTo(4);
        assertThat(balance.annualRemainingDays()).isEqualTo(20);
        assertThat(balance.annualReservedDays()).isZero();
    }

    @Test
    void approvedPaidLeaveDoesNotReduceAnnualBalance() {
        submitPaidLeave(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 7));
        leaveApprovalService.decide(managerId, firstRequestId(), RequestStatus.APPROVED, null);

        LeaveBalance balance = leaveService.balance(employeeId, 2026);
        assertThat(balance.paidUsedDays()).isEqualTo(3);
        assertThat(balance.annualUsedDays()).isZero();
        assertThat(balance.annualRemainingDays()).isEqualTo(20);
    }

    private void submitPaidLeave(LocalDate start, LocalDate end) {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setType(LeaveType.PAID_LEAVE);
        form.setStartDate(start);
        form.setEndDate(end);
        leaveService.submit(employeeId, form);
    }

    private void submit(LocalDate start, LocalDate end) {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setStartDate(start);
        form.setEndDate(end);
        leaveService.submit(employeeId, form);
    }

    private Long firstRequestId() {
        return leaveService.myRequests(employeeId).getFirst().id();
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
