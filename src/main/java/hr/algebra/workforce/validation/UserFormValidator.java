package hr.algebra.workforce.validation;

import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.form.UserForm;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.UserRepository;
import hr.algebra.workforce.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserFormValidator {

    private final UserRepository userRepository;
    private final UserAdminService userAdminService;

    public void validate(UserForm form, Errors errors) {
        validateEmail(form, errors);
        validatePassword(form, errors);
        validateManager(form, errors);
    }

    private void validateEmail(UserForm form, Errors errors) {
        if (form.getEmail() == null || form.getEmail().isBlank()) {
            return;
        }
        Optional<User> existing = userRepository.findByEmailIgnoreCase(form.getEmail().trim());
        if (existing.isPresent() && !existing.get().getId().equals(form.getId())) {
            errors.rejectValue("email", "duplicate", "Korisnik s tom e-poštom već postoji.");
        }
    }

    private void validatePassword(UserForm form, Errors errors) {
        if (form.getId() == null && (form.getPassword() == null || form.getPassword().isBlank())) {
            errors.rejectValue("password", "required", "Pri stvaranju korisnika lozinka je obavezna.");
        }
    }

    private void validateManager(UserForm form, Errors errors) {
        Long managerId = form.getManagerId();
        if (managerId == null) {
            if (form.getRole() == Role.EMPLOYEE) {
                errors.rejectValue("managerId", "required", "Zaposlenik mora imati voditelja.");
            }
            return;
        }
        if (managerId.equals(form.getId())) {
            errors.rejectValue("managerId", "self", "Korisnik ne može biti sam sebi voditelj.");
            return;
        }
        Optional<User> manager = userRepository.findById(managerId);
        if (manager.isEmpty() || !manager.get().isActive()) {
            errors.rejectValue("managerId", "inactive", "Odabrani voditelj nije dostupan.");
            return;
        }
        if (manager.get().getRole() == Role.EMPLOYEE) {
            errors.rejectValue("managerId", "role", "Voditelj mora imati ulogu voditelja ili administratora.");
            return;
        }
        if (userAdminService.createsManagerCycle(form.getId(), managerId)) {
            errors.rejectValue("managerId", "cycle", "Odabir stvara kružnu vezu nadređenih.");
        }
    }
}
