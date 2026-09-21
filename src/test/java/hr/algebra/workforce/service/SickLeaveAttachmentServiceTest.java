package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.SickLeaveRow;
import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.SickLeaveAttachment;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SickLeaveAttachmentServiceTest extends AbstractIntegrationTest {

    private static final byte[] PDF_BYTES = "%PDF-1.4 potvrda".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private SickLeaveService sickLeaveService;

    @Autowired
    private SickLeaveAttachmentService attachmentService;

    private Long managerId;
    private Long employeeId;
    private Long colleagueId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        adminId = createUser("admin@test.hr", Role.ADMIN, null).getId();
        User manager = createUser("voditelj@test.hr", Role.MANAGER, null);
        managerId = manager.getId();
        employeeId = createUser("zaposlenik@test.hr", Role.EMPLOYEE, manager).getId();
        colleagueId = createUser("kolega@test.hr", Role.EMPLOYEE, manager).getId();
    }

    @Test
    void attachedCertificateIsStoredAndReadableByOwner() {
        reportWithAttachment(pdf("doznaka.pdf"));
        Long leaveId = firstLeaveId(employeeId);

        SickLeaveAttachment attachment = attachmentService.download(employeeId, Role.EMPLOYEE, leaveId);

        assertThat(attachment.getFileName()).isEqualTo("doznaka.pdf");
        assertThat(attachment.getContentType()).isEqualTo("application/pdf");
        assertThat(attachment.getFileSize()).isEqualTo(PDF_BYTES.length);
        assertThat(attachment.getContent()).isEqualTo(PDF_BYTES);
    }

    @Test
    void rowSignalsWhetherAttachmentExists() {
        reportWithAttachment(pdf("doznaka.pdf"));
        SickLeaveForm withoutFile = new SickLeaveForm();
        withoutFile.setStartDate(LocalDate.of(2026, 8, 3));
        withoutFile.setEndDate(LocalDate.of(2026, 8, 4));
        sickLeaveService.report(colleagueId, withoutFile);

        assertThat(sickLeaveService.myLeaves(employeeId)).singleElement()
                .extracting(SickLeaveRow::hasAttachment).isEqualTo(true);
        assertThat(sickLeaveService.myLeaves(colleagueId)).singleElement()
                .extracting(SickLeaveRow::hasAttachment).isEqualTo(false);
    }

    @Test
    void ownManagerAndAdminMayDownloadButColleagueMayNot() {
        reportWithAttachment(pdf("doznaka.pdf"));
        Long leaveId = firstLeaveId(employeeId);

        assertThat(attachmentService.download(managerId, Role.MANAGER, leaveId)).isNotNull();
        assertThat(attachmentService.download(adminId, Role.ADMIN, leaveId)).isNotNull();
        assertThatThrownBy(() -> attachmentService.download(colleagueId, Role.EMPLOYEE, leaveId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void disallowedContentTypeIsRejected() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.of(2026, 9, 18));
        form.setAttachment(new MockMultipartFile("attachment", "virus.exe",
                "application/octet-stream", new byte[]{1, 2, 3}));

        assertThatThrownBy(() -> sickLeaveService.report(employeeId, form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PDF");
        assertThat(sickLeaveService.myLeaves(employeeId)).isEmpty();
    }

    @Test
    void oversizedFileIsRejected() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.of(2026, 9, 18));
        form.setAttachment(new MockMultipartFile("attachment", "velika.pdf", "application/pdf",
                new byte[(int) AttachmentPolicy.MAX_SIZE_BYTES + 1]));

        assertThatThrownBy(() -> sickLeaveService.report(employeeId, form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void reportWithoutAttachmentStillWorks() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.of(2026, 9, 18));
        sickLeaveService.report(employeeId, form);

        Long leaveId = firstLeaveId(employeeId);
        assertThat(attachmentService.hasAttachment(leaveId)).isFalse();
        assertThatThrownBy(() -> attachmentService.download(employeeId, Role.EMPLOYEE, leaveId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pathTraversalInFileNameIsStripped() {
        reportWithAttachment(new MockMultipartFile("attachment", "../../etc/tajna.pdf",
                "application/pdf", PDF_BYTES));

        assertThat(attachmentService.download(employeeId, Role.EMPLOYEE, firstLeaveId(employeeId)).getFileName())
                .isEqualTo("tajna.pdf");
    }

    private void reportWithAttachment(MockMultipartFile file) {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(LocalDate.of(2026, 9, 18));
        form.setEndDate(LocalDate.of(2026, 9, 19));
        form.setDocumentReference("DZ-1");
        form.setAttachment(file);
        sickLeaveService.report(employeeId, form);
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("attachment", name, "application/pdf", PDF_BYTES);
    }

    private Long firstLeaveId(Long userId) {
        return sickLeaveService.myLeaves(userId).getFirst().id();
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
