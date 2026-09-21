package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.dto.MonthlyReport;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.security.JwtPrincipal;
import hr.algebra.workforce.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportRestController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public MonthlyReport monthly(@AuthenticationPrincipal JwtPrincipal principal,
                                 Authentication authentication,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return reportService.monthlyReport(principal.id(), roleOf(authentication),
                month == null ? YearMonth.now() : month);
    }

    private Role roleOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElse(Role.EMPLOYEE);
    }
}
