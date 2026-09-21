package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.TeamCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.YearMonth;

@Controller
@RequestMapping("/team/calendar")
@RequiredArgsConstructor
public class TeamCalendarController {

    private final TeamCalendarService teamCalendarService;

    @GetMapping
    public String calendar(@AuthenticationPrincipal AppUserDetails principal,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                           Model model) {
        YearMonth selected = month == null ? YearMonth.now() : month;
        model.addAttribute("calendar", teamCalendarService.monthlyCalendar(principal.getId(),
                principal.getRole(), selected));
        model.addAttribute("month", selected);
        model.addAttribute("previousMonth", selected.minusMonths(1));
        model.addAttribute("nextMonth", selected.plusMonths(1));
        return "team-calendar";
    }
}
