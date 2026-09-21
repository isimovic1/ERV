package hr.algebra.workforce.validation;

import hr.algebra.workforce.dto.LeaveBalance;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class LeaveRequestFormValidator {

    private final LeaveService leaveService;

    public void validate(LeaveRequestForm form, Errors errors, Long userId) {
        LocalDate start = form.getStartDate();
        LocalDate end = form.getEndDate();
        if (start == null || end == null) {
            return;
        }
        if (end.isBefore(start)) {
            errors.rejectValue("endDate", "range", "Datum završetka mora biti nakon datuma početka.");
            return;
        }
        int requestedDays = leaveService.workingDaysBetween(start, end);
        if (requestedDays == 0) {
            errors.rejectValue("startDate", "weekend", "Odabrani raspon ne sadrži ni jedan radni dan.");
            return;
        }
        if (leaveService.overlapsExistingRequest(userId, start, end)) {
            errors.rejectValue("startDate", "overlap", "Raspon se preklapa s postojećim zahtjevom.");
            return;
        }
        LeaveBalance balance = leaveService.balance(userId, start.getYear());
        int remaining = leaveService.remainingDaysFor(balance, form.getType());
        if (requestedDays > remaining) {
            String label = form.getType() == LeaveType.PAID_LEAVE ? "plaćenog dopusta" : "godišnjeg odmora";
            errors.rejectValue("endDate", "balance",
                    "Nemate dovoljno preostalih dana " + label + " (preostalo: " + remaining + ").");
        }
    }
}
