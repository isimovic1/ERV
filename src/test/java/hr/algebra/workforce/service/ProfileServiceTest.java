package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.form.PasswordChangeForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileServiceTest extends AbstractIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Korisnik");
        user.setEmail("test@test.hr");
        user.setPassword(passwordEncoder.encode("Algebra1!"));
        user.setRole(Role.EMPLOYEE);
        user.setHireDate(LocalDate.of(2025, 1, 1));
        userId = userRepository.save(user).getId();
    }

    @Test
    void passwordIsChangedWhenCurrentOneMatches() {
        profileService.changePassword(userId, form("Algebra1!", "NovaLozinka1!"));

        User stored = userRepository.findById(userId).orElseThrow();
        assertThat(passwordEncoder.matches("NovaLozinka1!", stored.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Algebra1!", stored.getPassword())).isFalse();
    }

    @Test
    void wrongCurrentPasswordIsRejected() {
        assertThatThrownBy(() -> profileService.changePassword(userId, form("Pogresna1!", "NovaLozinka1!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nije ispravna");

        assertThat(passwordEncoder.matches("Algebra1!",
                userRepository.findById(userId).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void reusingSamePasswordIsRejected() {
        assertThatThrownBy(() -> profileService.changePassword(userId, form("Algebra1!", "Algebra1!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("različita");
    }

    private PasswordChangeForm form(String current, String next) {
        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword(current);
        form.setNewPassword(next);
        form.setConfirmPassword(next);
        return form;
    }
}
