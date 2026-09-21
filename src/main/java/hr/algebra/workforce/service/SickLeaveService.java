package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.SickLeaveRow;
import hr.algebra.workforce.event.NotificationEvent;
import hr.algebra.workforce.event.NotificationType;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.SickLeaveCloseForm;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.model.SickLeaveStatus;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.SickLeaveRepository;
import hr.algebra.workforce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SickLeaveService {

    private final SickLeaveRepository sickLeaveRepository;
    private final UserRepository userRepository;
    private final SickLeaveAttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<SickLeaveRow> myLeaves(Long userId) {
        return sickLeaveRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(leave -> SickLeaveRow.of(leave, attachmentService.hasAttachment(leave.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasOverlappingLeave(Long userId, LocalDate start, LocalDate end) {
        return sickLeaveRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .anyMatch(leave -> overlaps(leave, start, end));
    }

    @Transactional
    public void report(Long userId, SickLeaveForm form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik ne postoji: " + userId));
        SickLeave leave = new SickLeave();
        leave.setUser(user);
        leave.setStartDate(form.getStartDate());
        leave.setEndDate(form.getEndDate());
        leave.setDescription(form.getDescription());
        leave.setDocumentReference(form.getDocumentReference());
        leave.setStatus(form.getEndDate() == null ? SickLeaveStatus.REPORTED : SickLeaveStatus.CLOSED);
        sickLeaveRepository.save(leave);
        attachmentService.store(leave, form.getAttachment());
        publishReported(leave);
        log.debug("Prijavljeno bolovanje od {} za korisnika {}", form.getStartDate(), userId);
    }

    @Transactional
    public void close(Long userId, Long leaveId, SickLeaveCloseForm form) {
        SickLeave leave = requireOwnedLeave(userId, leaveId);
        if (leave.getStatus() == SickLeaveStatus.CLOSED) {
            throw new ResourceNotFoundException("Bolovanje je već zatvoreno: " + leaveId);
        }
        if (form.getEndDate().isBefore(leave.getStartDate())) {
            throw new IllegalArgumentException("Datum završetka je prije početka bolovanja.");
        }
        leave.setEndDate(form.getEndDate());
        leave.setStatus(SickLeaveStatus.CLOSED);
    }

    private void publishReported(SickLeave leave) {
        User manager = leave.getUser().getManager();
        if (manager == null) {
            log.warn("Zaposlenik {} nema voditelja, obavijest nije objavljena", leave.getUser().getId());
            return;
        }
        eventPublisher.publishEvent(NotificationEvent.of(NotificationType.SICK_LEAVE_REPORTED,
                leave.getUser().getId(), leave.getUser().getFullName(), manager.getEmail(),
                "Prijavljeno bolovanje",
                leave.getUser().getFullName() + " je prijavio bolovanje od " + leave.getStartDate() + "."));
    }

    private SickLeave requireOwnedLeave(Long userId, Long leaveId) {
        return sickLeaveRepository.findById(leaveId)
                .filter(leave -> leave.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Bolovanje ne postoji: " + leaveId));
    }

    private boolean overlaps(SickLeave leave, LocalDate start, LocalDate end) {
        LocalDate leaveEnd = leave.getEndDate() == null ? LocalDate.MAX : leave.getEndDate();
        LocalDate requestedEnd = end == null ? LocalDate.MAX : end;
        return !start.isAfter(leaveEnd) && !requestedEnd.isBefore(leave.getStartDate());
    }
}
