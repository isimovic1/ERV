package hr.algebra.workforce.dto;

import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.model.SickLeaveStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SickLeaveRow(Long id,
                           String employeeName,
                           LocalDate startDate,
                           LocalDate endDate,
                           SickLeaveStatus status,
                           String description,
                           String documentReference,
                           LocalDateTime reportedAt,
                           boolean hasAttachment) {

    public static SickLeaveRow of(SickLeave leave, boolean hasAttachment) {
        return new SickLeaveRow(leave.getId(), leave.getUser().getFullName(), leave.getStartDate(),
                leave.getEndDate(), leave.getStatus(), leave.getDescription(),
                leave.getDocumentReference(), leave.getReportedAt(), hasAttachment);
    }
}
