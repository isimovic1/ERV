package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.UserRow;
import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.UserForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserRow> allUsers() {
        return userRepository.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .map(UserRow::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserRow> availableManagers() {
        return userRepository.findByActiveTrueOrderByLastNameAsc().stream()
                .filter(user -> user.getRole() != Role.EMPLOYEE)
                .map(UserRow::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserForm findForEdit(Long userId) {
        User user = requireUser(userId);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setManagerId(user.getManager() == null ? null : user.getManager().getId());
        form.setHireDate(user.getHireDate());
        form.setVacationDaysPerYear(user.getVacationDaysPerYear());
        form.setActive(user.isActive());
        return form;
    }

    @Transactional
    public void save(UserForm form) {
        User user = form.getId() == null ? new User() : requireUser(form.getId());
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setRole(form.getRole());
        user.setManager(form.getManagerId() == null ? null : requireUser(form.getManagerId()));
        user.setHireDate(form.getHireDate());
        user.setVacationDaysPerYear(form.getVacationDaysPerYear());
        user.setActive(Boolean.TRUE.equals(form.getActive()));
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        userRepository.save(user);
        log.info("Spremljen korisnik {}", user.getEmail());
    }

    @Transactional
    public void setActive(Long actorId, Long targetId, boolean active) {
        User user = requireUser(targetId);
        if (!active) {
            requireDeactivationAllowed(actorId, user);
        }
        user.setActive(active);
        if (!active) {
            userRepository.findByManagerId(targetId).forEach(subordinate -> subordinate.setManager(null));
        }
        log.info("Korisnik {} je {}", user.getEmail(), active ? "aktiviran" : "deaktiviran");
    }

    private void requireDeactivationAllowed(Long actorId, User user) {
        if (user.getId().equals(actorId)) {
            throw new BusinessRuleException("Ne možete deaktivirati vlastiti korisnički račun.");
        }
        if (user.getRole() == Role.ADMIN && user.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("Sustav mora imati barem jednog aktivnog administratora.");
        }
    }

    @Transactional(readOnly = true)
    public boolean createsManagerCycle(Long userId, Long managerId) {
        if (userId == null || managerId == null) {
            return false;
        }
        User current = userRepository.findById(managerId).orElse(null);
        while (current != null) {
            if (current.getId().equals(userId)) {
                return true;
            }
            current = current.getManager();
        }
        return false;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik ne postoji: " + userId));
    }
}
