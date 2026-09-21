package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.SickLeaveRow;
import hr.algebra.workforce.event.NotificationEvent;
import hr.algebra.workforce.event.NotificationType;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.model.SickLeaveStatus;
import hr.algebra.workforce.repository.SickLeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SickLeaveReviewService {

    private final SickLeaveRepository sickLeaveRepository;
    private final SickLeaveAttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<SickLeaveRow> teamLeaves(Long managerId) {
        return sickLeaveRepository.findByUserManagerIdOrderByStartDateDesc(managerId).stream()
                .map(leave -> SickLeaveRow.of(leave, attachmentService.hasAttachment(leave.getId())))
                .toList();
    }

    @Transactional
    public void confirm(Long managerId, Long leaveId) {
        SickLeave leave = sickLeaveRepository.findById(leaveId)
                .filter(candidate -> candidate.getStatus() == SickLeaveStatus.REPORTED)
                .filter(candidate -> candidate.getUser().getManager() != null)
                .filter(candidate -> candidate.getUser().getManager().getId().equals(managerId))
                .orElseThrow(() -> new ResourceNotFoundException("Bolovanje nije dostupno za potvrdu: " + leaveId));
        leave.setStatus(SickLeaveStatus.CONFIRMED);
        eventPublisher.publishEvent(NotificationEvent.of(NotificationType.SICK_LEAVE_CONFIRMED,
                leave.getUser().getId(), leave.getUser().getFullName(), leave.getUser().getEmail(),
                "Bolovanje je potvrđeno",
                "Vaše bolovanje od " + leave.getStartDate() + " je potvrđeno."));
        log.info("Bolovanje {} je potvrđeno", leaveId);
    }
}
