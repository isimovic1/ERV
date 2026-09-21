package hr.algebra.workforce.dto;

import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.model.LeaveRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestRow(Long id,
                                 String employeeName,
                                 LeaveType type,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 int workingDays,
                                 RequestStatus status,
                                 String note,
                                 String decisionNote,
                                 LocalDateTime submittedAt) {

    public static LeaveRequestRow of(LeaveRequest request) {
        return new LeaveRequestRow(request.getId(), request.getUser().getFullName(), request.getType(),
                request.getStartDate(),
                request.getEndDate(), request.getWorkingDays(), request.getStatus(), request.getNote(),
                request.getDecisionNote(), request.getSubmittedAt());
    }
}
