package hr.algebra.workforce.service;

import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.PasswordChangeForm;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(Long userId, PasswordChangeForm form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik ne postoji: " + userId));
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Trenutna lozinka nije ispravna.");
        }
        if (passwordEncoder.matches(form.getNewPassword(), user.getPassword())) {
            throw new BusinessRuleException("Nova lozinka mora biti različita od trenutne.");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
        log.info("Korisnik {} je promijenio lozinku", user.getEmail());
    }
}
