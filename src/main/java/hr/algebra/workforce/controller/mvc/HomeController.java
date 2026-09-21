package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("dashboard", dashboardService.forUser(principal.getId(), principal.getFullName(),
                principal.getRole()));
        return "home";
    }
}
