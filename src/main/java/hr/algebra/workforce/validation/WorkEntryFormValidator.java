package hr.algebra.workforce.validation;

import hr.algebra.workforce.form.WorkEntryForm;
import hr.algebra.workforce.service.AbsenceService;
import hr.algebra.workforce.service.WorkEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class WorkEntryFormValidator {

    private final WorkEntryService workEntryService;
    private final AbsenceService absenceService;

    public void validate(WorkEntryForm form, Errors errors, Long userId) {
        LocalDate date = form.getWorkDate();
        if (date == null) {
            return;
        }
        if (workEntryService.existsForDate(userId, date, form.getId())) {
            errors.rejectValue("workDate", "duplicate", "Za taj datum već postoji unos radnih sati.");
        }
        if (absenceService.isOnApprovedVacation(userId, date)) {
            errors.rejectValue("workDate", "vacation", "Na taj datum imate odobren godišnji odmor.");
        }
        if (absenceService.isOnSickLeave(userId, date)) {
            errors.rejectValue("workDate", "sickLeave", "Na taj datum ste prijavljeni na bolovanje.");
        }
    }
}
