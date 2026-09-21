package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.dto.MonthlyWorkSummary;
import hr.algebra.workforce.dto.WorkEntryRequest;
import hr.algebra.workforce.form.WorkEntryForm;
import hr.algebra.workforce.security.JwtPrincipal;
import hr.algebra.workforce.service.WorkEntryService;
import hr.algebra.workforce.validation.WorkEntryFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/work-entries")
@RequiredArgsConstructor
public class WorkEntryRestController {

    private final WorkEntryService workEntryService;
    private final WorkEntryFormValidator validator;
    private final RestValidationSupport validationSupport;

    @GetMapping
    public MonthlyWorkSummary monthly(@AuthenticationPrincipal JwtPrincipal principal,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return workEntryService.monthlySummary(principal.id(), month == null ? YearMonth.now() : month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonthlyWorkSummary create(@AuthenticationPrincipal JwtPrincipal principal,
                                     @Valid @RequestBody WorkEntryRequest request) {
        WorkEntryForm form = request.toForm();
        validationSupport.validate(form, (target, errors) -> validator.validate(target, errors, principal.id()));
        workEntryService.save(principal.id(), form);
        return workEntryService.monthlySummary(principal.id(), YearMonth.from(form.getWorkDate()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        workEntryService.delete(principal.id(), id);
    }
}
