package hr.algebra.workforce.configuration;

import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("!dev")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.error("Baza nema nijednog korisnika, a ADMIN_EMAIL i ADMIN_PASSWORD nisu postavljeni. "
                    + "Prijava u aplikaciju nije moguća.");
            return;
        }
        User admin = new User();
        admin.setFirstName("Administrator");
        admin.setLastName("Sustava");
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setHireDate(LocalDate.now());
        admin.setVacationDaysPerYear(20);
        userRepository.save(admin);
        log.info("Kreiran početni administrator {}", admin.getEmail());
    }
}
