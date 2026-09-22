package hr.algebra.workforce.configuration;

import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.password}")
    private String seedPassword;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = createUser("Ivan", "Šimović", "admin@erv.hr", Role.ADMIN, null);
        User manager = createUser("Petra", "Kovač", "voditelj@erv.hr", Role.MANAGER, admin);
        createUser("Marko", "Horvat", "marko@erv.hr", Role.EMPLOYEE, manager);
        createUser("Ana", "Babić", "ana@erv.hr", Role.EMPLOYEE, manager);
        log.info("Početni korisnici kreirani");
    }

    private User createUser(String firstName, String lastName, String email, Role role, User manager) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(seedPassword));
        user.setRole(role);
        user.setHireDate(LocalDate.of(2024, 1, 15));
        user.setVacationDaysPerYear(20);
        user.setManager(manager);
        return userRepository.save(user);
    }
}
