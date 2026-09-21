package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.dto.SickLeaveRequestPayload;
import hr.algebra.workforce.dto.SickLeaveRow;
import hr.algebra.workforce.form.SickLeaveCloseForm;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.security.JwtPrincipal;
import hr.algebra.workforce.service.SickLeaveService;
import hr.algebra.workforce.validation.SickLeaveFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sick-leaves")
@RequiredArgsConstructor
public class SickLeaveRestController {

    private final SickLeaveService sickLeaveService;
    private final SickLeaveFormValidator validator;
    private final RestValidationSupport validationSupport;

    @GetMapping
    public List<SickLeaveRow> myLeaves(@AuthenticationPrincipal JwtPrincipal principal) {
        return sickLeaveService.myLeaves(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<SickLeaveRow> report(@AuthenticationPrincipal JwtPrincipal principal,
                                     @Valid @RequestBody SickLeaveRequestPayload payload) {
        SickLeaveForm form = payload.toForm();
        validationSupport.validate(form, (target, errors) -> validator.validate(target, errors, principal.id()));
        sickLeaveService.report(principal.id(), form);
        return sickLeaveService.myLeaves(principal.id());
    }

    @PostMapping("/{id}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@AuthenticationPrincipal JwtPrincipal principal,
                      @PathVariable Long id,
                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        SickLeaveCloseForm form = new SickLeaveCloseForm();
        form.setEndDate(endDate);
        sickLeaveService.close(principal.id(), id, form);
    }
}
