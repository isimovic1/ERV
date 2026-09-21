package hr.algebra.workforce.kafka;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.event.NotificationType;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.model.NotificationLog;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.NotificationLogRepository;
import hr.algebra.workforce.service.SickLeaveService;
import hr.algebra.workforce.service.LeaveApprovalService;
import hr.algebra.workforce.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@EmbeddedKafka(partitions = 1, topics = "workforce.notifications")
class NotificationFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveApprovalService leaveApprovalService;

    @Autowired
    private SickLeaveService sickLeaveService;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    private Long managerId;
    private Long employeeId;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        managerId = manager.getId();
        employeeId = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager).getId();
    }

    @Test
    void submittedVacationReachesManagerThroughKafka() {
        submitVacation();

        List<NotificationLog> logs = awaitLogs(1);
        assertThat(logs.getFirst().getType()).isEqualTo(NotificationType.VACATION_REQUESTED);
        assertThat(logs.getFirst().getRecipientEmail()).isEqualTo("voditelj@test.hr");
        assertThat(logs.getFirst().isDelivered()).isTrue();
        assertThat(logs.getFirst().getMessage()).contains("radnih dana");
    }

    @Test
    void approvalNotifiesEmployee() {
        submitVacation();
        awaitLogs(1);

        leaveApprovalService.decide(managerId, leaveService.myRequests(employeeId).getFirst().id(),
                RequestStatus.APPROVED, "U redu");

        List<NotificationLog> logs = awaitLogs(2);
        assertThat(logs).extracting(NotificationLog::getType)
                .containsExactlyInAnyOrder(NotificationType.VACATION_REQUESTED, NotificationType.VACATION_APPROVED);
        assertThat(logs).filteredOn(entry -> entry.getType() == NotificationType.VACATION_APPROVED)
                .singleElement()
                .satisfies(entry -> assertThat(entry.getRecipientEmail()).isEqualTo("zaposlenik@test.hr"));
    }

    @Test
    void reportedSickLeaveNotifiesManager() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.now().minusDays(1));
        sickLeaveService.report(employeeId, form);

        List<NotificationLog> logs = awaitLogs(1);
        assertThat(logs.getFirst().getType()).isEqualTo(NotificationType.SICK_LEAVE_REPORTED);
        assertThat(logs.getFirst().getRecipientEmail()).isEqualTo("voditelj@test.hr");
    }

    private void submitVacation() {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setStartDate(LocalDate.now().plusDays(10));
        form.setEndDate(LocalDate.now().plusDays(12));
        leaveService.submit(employeeId, form);
    }

    private List<NotificationLog> awaitLogs(int expectedCount) {
        await().atMost(Duration.ofSeconds(20))
                .until(() -> notificationLogRepository.count() >= expectedCount);
        return notificationLogRepository.findAll();
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
