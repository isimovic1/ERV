package hr.algebra.workforce.configuration;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "app.bootstrap.admin-email=sef@erv.hr",
        "app.bootstrap.admin-password=PocetnaLozinka1!"
})
class AdminBootstrapTest extends AbstractIntegrationTest {

    @Autowired
    private AdminBootstrap adminBootstrap;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void administratorIsCreatedOnEmptyDatabase() {
        adminBootstrap.run();

        User admin = userRepository.findByEmailIgnoreCase("sef@erv.hr").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isActive()).isTrue();
        assertThat(passwordEncoder.matches("PocetnaLozinka1!", admin.getPassword())).isTrue();
    }

    @Test
    void existingUsersAreNeverOverwritten() {
        User existing = new User();
        existing.setFirstName("Postojeci");
        existing.setLastName("Korisnik");
        existing.setEmail("postojeci@erv.hr");
        existing.setPassword(passwordEncoder.encode("Algebra1!"));
        existing.setRole(Role.EMPLOYEE);
        existing.setHireDate(LocalDate.of(2025, 1, 1));
        userRepository.save(existing);

        adminBootstrap.run();

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmailIgnoreCase("sef@erv.hr")).isEmpty();
    }

    @Test
    void missingCredentialsLeaveDatabaseEmpty() {
        ReflectionTestUtils.setField(adminBootstrap, "adminPassword", "");

        adminBootstrap.run();

        assertThat(userRepository.count()).isZero();
    }
}
