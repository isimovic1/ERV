package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.dto.UserRow;
import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.form.UserForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.validation.UserFormValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAdminServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private UserFormValidator userFormValidator;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long adminId;
    private Long managerId;

    @BeforeEach
    void setUp() {
        adminId = createUser("admin@test.hr", Role.ADMIN, null).getId();
        managerId = createUser("voditelj@test.hr", Role.MANAGER, null).getId();
    }

    @Test
    void savedUserGetsHashedPasswordAndNormalisedEmail() {
        userAdminService.save(form("  Ivo ", "Ivić", "  IVO@Workforce.HR ", Role.EMPLOYEE, managerId, "Algebra1!"));

        User stored = userRepository.findByEmailIgnoreCase("ivo@workforce.hr").orElseThrow();
        assertThat(stored.getEmail()).isEqualTo("ivo@workforce.hr");
        assertThat(stored.getFirstName()).isEqualTo("Ivo");
        assertThat(stored.getLastName()).isEqualTo("Ivić");
        assertThat(stored.getPassword()).isNotEqualTo("Algebra1!");
        assertThat(passwordEncoder.matches("Algebra1!", stored.getPassword())).isTrue();
    }

    @Test
    void editWithoutPasswordKeepsPreviousPassword() {
        userAdminService.save(form("Ivo", "Ivić", "ivo@test.hr", Role.EMPLOYEE, managerId, "Algebra1!"));
        Long userId = userRepository.findByEmailIgnoreCase("ivo@test.hr").orElseThrow().getId();

        UserForm edit = userAdminService.findForEdit(userId);
        edit.setFirstName("Ivan");
        userAdminService.save(edit);

        User stored = userRepository.findById(userId).orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("Ivan");
        assertThat(passwordEncoder.matches("Algebra1!", stored.getPassword())).isTrue();
    }

    @Test
    void ownAccountCannotBeDeactivated() {
        assertThatThrownBy(() -> userAdminService.setActive(adminId, adminId, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vlastiti");
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() {
        Long secondAdminId = createUser("admin2@test.hr", Role.ADMIN, null).getId();

        userAdminService.setActive(secondAdminId, adminId, false);

        assertThatThrownBy(() -> userAdminService.setActive(adminId, secondAdminId, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("administratora");
    }

    @Test
    void deactivatingManagerDetachesSubordinates() {
        userAdminService.save(form("Ivo", "Ivić", "ivo@test.hr", Role.EMPLOYEE, managerId, "Algebra1!"));

        userAdminService.setActive(adminId, managerId, false);

        UserRow subordinate = userAdminService.allUsers().stream()
                .filter(row -> row.email().equals("ivo@test.hr"))
                .findFirst()
                .orElseThrow();
        assertThat(subordinate.managerName()).isNull();
    }

    @Test
    void inactiveUserIsNotOfferedAsManager() {
        assertThat(userAdminService.availableManagers()).hasSize(2);

        userAdminService.setActive(adminId, managerId, false);

        assertThat(userAdminService.availableManagers()).hasSize(1);
    }

    @Test
    void managerCycleIsRejected() {
        userAdminService.save(form("Ivo", "Ivić", "ivo@test.hr", Role.MANAGER, managerId, "Algebra1!"));
        Long middleId = userRepository.findByEmailIgnoreCase("ivo@test.hr").orElseThrow().getId();

        UserForm topLevel = userAdminService.findForEdit(managerId);
        topLevel.setManagerId(middleId);
        Errors errors = new BeanPropertyBindingResult(topLevel, "userForm");

        userFormValidator.validate(topLevel, errors);

        assertThat(errors.getFieldError("managerId")).isNotNull();
        assertThat(errors.getFieldError("managerId").getDefaultMessage()).contains("kružnu");
    }

    @Test
    void employeeWithoutManagerIsRejected() {
        UserForm form = form("Bez", "Voditelja", "bez@test.hr", Role.EMPLOYEE, null, "Algebra1!");
        Errors errors = new BeanPropertyBindingResult(form, "userForm");

        userFormValidator.validate(form, errors);

        assertThat(errors.getFieldError("managerId")).isNotNull();
    }

    private UserForm form(String firstName, String lastName, String email, Role role, Long managerId,
                          String password) {
        UserForm form = new UserForm();
        form.setFirstName(firstName);
        form.setLastName(lastName);
        form.setEmail(email);
        form.setRole(role);
        form.setManagerId(managerId);
        form.setHireDate(LocalDate.of(2026, 3, 1));
        form.setVacationDaysPerYear(20);
        form.setActive(Boolean.TRUE);
        form.setPassword(password);
        return form;
    }

    private User createUser(String email, Role role, User manager) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName(role.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Algebra1!"));
        user.setRole(role);
        user.setHireDate(LocalDate.of(2025, 1, 1));
        user.setManager(manager);
        return userRepository.save(user);
    }
}
