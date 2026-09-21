package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.dto.LeaveBalance;
import hr.algebra.workforce.dto.LeaveRequestPayload;
import hr.algebra.workforce.dto.LeaveRequestRow;
import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.security.JwtPrincipal;
import hr.algebra.workforce.service.LeaveService;
import hr.algebra.workforce.validation.LeaveRequestFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRestController {

    private final LeaveService leaveService;
    private final LeaveRequestFormValidator validator;
    private final RestValidationSupport validationSupport;

    @GetMapping
    public List<LeaveRequestRow> myRequests(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.myRequests(principal.id());
    }

    @GetMapping("/balance")
    public LeaveBalance balance(@AuthenticationPrincipal JwtPrincipal principal,
                                   @RequestParam(required = false) Integer year) {
        return leaveService.balance(principal.id(), year == null ? LocalDate.now().getYear() : year);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<LeaveRequestRow> submit(@AuthenticationPrincipal JwtPrincipal principal,
                                           @Valid @RequestBody LeaveRequestPayload payload) {
        LeaveRequestForm form = payload.toForm();
        validationSupport.validate(form, (target, errors) -> validator.validate(target, errors, principal.id()));
        leaveService.submit(principal.id(), form);
        return leaveService.myRequests(principal.id());
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable Long id) {
        leaveService.cancel(principal.id(), id);
    }
}
