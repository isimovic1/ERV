package hr.algebra.workforce.validation;

import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.service.AbsenceService;
import hr.algebra.workforce.service.SickLeaveService;
import hr.algebra.workforce.service.WorkEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SickLeaveFormValidator {

    private final SickLeaveService sickLeaveService;
    private final AbsenceService absenceService;
    private final WorkEntryService workEntryService;

    public void validate(SickLeaveForm form, Errors errors, Long userId) {
        LocalDate start = form.getStartDate();
        if (start == null) {
            return;
        }
        LocalDate end = form.getEndDate();
        if (end != null && end.isBefore(start)) {
            errors.rejectValue("endDate", "range", "Datum završetka mora biti nakon datuma početka.");
            return;
        }
        if (sickLeaveService.hasOverlappingLeave(userId, start, end)) {
            errors.rejectValue("startDate", "overlap", "Razdoblje se preklapa s već prijavljenim bolovanjem.");
            return;
        }
        LocalDate effectiveEnd = end == null ? start : end;
        if (absenceService.hasApprovedVacationBetween(userId, start, effectiveEnd)) {
            errors.rejectValue("startDate", "vacation",
                    "Na to razdoblje imate odobren godišnji odmor. Zatražite izmjenu godišnjeg kod voditelja.");
            return;
        }
        if (workEntryService.hasEntriesBetween(userId, start, effectiveEnd)) {
            errors.rejectValue("startDate", "workEntries",
                    "Za to razdoblje već postoje uneseni radni sati. Obrišite ih prije prijave bolovanja.");
        }
    }
}
