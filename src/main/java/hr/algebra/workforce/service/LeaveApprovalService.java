package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.LeaveRequestRow;
import hr.algebra.workforce.event.NotificationEvent;
import hr.algebra.workforce.event.NotificationType;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.model.LeaveRequest;
import hr.algebra.workforce.repository.UserRepository;
import hr.algebra.workforce.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveApprovalService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<LeaveRequestRow> pendingForManager(Long managerId) {
        return leaveRequestRepository
                .findByUserManagerIdAndStatusOrderBySubmittedAtAsc(managerId, RequestStatus.PENDING)
                .stream()
                .map(LeaveRequestRow::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestRow> historyForManager(Long managerId) {
        return leaveRequestRepository.findByUserManagerIdOrderBySubmittedAtDesc(managerId).stream()
                .filter(request -> request.getStatus() != RequestStatus.PENDING)
                .map(LeaveRequestRow::of)
                .toList();
    }

    @Transactional
    public void decide(Long managerId, Long requestId, RequestStatus decision, String decisionNote) {
        if (decision != RequestStatus.APPROVED && decision != RequestStatus.REJECTED) {
            throw new IllegalArgumentException("Nedozvoljena odluka: " + decision);
        }
        LeaveRequest request = requirePendingRequest(managerId, requestId);
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Voditelj ne postoji: " + managerId));
        request.setStatus(decision);
        request.setDecidedBy(manager);
        request.setDecidedAt(LocalDateTime.now());
        request.setDecisionNote(decisionNote);
        publishDecision(request, decision);
        log.info("Zahtjev {} je {}", requestId, decision);
    }

    private void publishDecision(LeaveRequest request, RequestStatus decision) {
        boolean approved = decision == RequestStatus.APPROVED;
        String outcome = approved ? "odobren" : "odbijen";
        eventPublisher.publishEvent(NotificationEvent.of(
                approved ? NotificationType.VACATION_APPROVED : NotificationType.VACATION_REJECTED,
                request.getUser().getId(), request.getUser().getFullName(), request.getUser().getEmail(),
                "Zahtjev za godišnji odmor je " + outcome,
                "Vaš zahtjev za razdoblje " + request.getStartDate() + " - " + request.getEndDate()
                        + " je " + outcome + "."));
    }

    private LeaveRequest requirePendingRequest(Long managerId, Long requestId) {
        return leaveRequestRepository.findById(requestId)
                .filter(request -> request.getStatus() == RequestStatus.PENDING)
                .filter(request -> request.getUser().getManager() != null)
                .filter(request -> request.getUser().getManager().getId().equals(managerId))
                .orElseThrow(() -> new ResourceNotFoundException("Zahtjev nije dostupan za odluku: " + requestId));
    }
}
