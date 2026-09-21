package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.model.LeaveType;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.LeaveService;
import hr.algebra.workforce.validation.LeaveRequestFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveService leaveService;
    private final LeaveRequestFormValidator validator;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        if (!model.containsAttribute("leaveRequestForm")) {
            model.addAttribute("leaveRequestForm", new LeaveRequestForm());
        }
        return render(principal, model);
    }

    @PostMapping
    public String submit(@AuthenticationPrincipal AppUserDetails principal,
                         @Valid @ModelAttribute LeaveRequestForm leaveRequestForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        validator.validate(leaveRequestForm, bindingResult, principal.getId());
        if (bindingResult.hasErrors()) {
            return render(principal, model);
        }
        leaveService.submit(principal.getId(), leaveRequestForm);
        redirectAttributes.addFlashAttribute("message", "Zahtjev je podnesen.");
        return "redirect:/leaves";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal AppUserDetails principal,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        leaveService.cancel(principal.getId(), id);
        redirectAttributes.addFlashAttribute("message", "Zahtjev je otkazan.");
        return "redirect:/leaves";
    }

    private String render(AppUserDetails principal, Model model) {
        model.addAttribute("balance", leaveService.balance(principal.getId(), LocalDate.now().getYear()));
        model.addAttribute("requests", leaveService.myRequests(principal.getId()));
        model.addAttribute("leaveTypes", LeaveType.values());
        return "leave-requests";
    }
}
