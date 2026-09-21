package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.VacationDecisionForm;
import hr.algebra.workforce.model.RequestStatus;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.LeaveApprovalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/team/requests")
@RequiredArgsConstructor
public class TeamLeaveController {

    private final LeaveApprovalService leaveApprovalService;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("pendingRequests", leaveApprovalService.pendingForManager(principal.getId()));
        model.addAttribute("decidedRequests", leaveApprovalService.historyForManager(principal.getId()));
        model.addAttribute("decisionForm", new VacationDecisionForm());
        return "team-requests";
    }

    @PostMapping("/{id}/decision")
    public String decide(@AuthenticationPrincipal AppUserDetails principal,
                         @PathVariable Long id,
                         @RequestParam RequestStatus decision,
                         @Valid @ModelAttribute VacationDecisionForm decisionForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Obrazloženje je predugačko.");
            return "redirect:/team/requests";
        }
        leaveApprovalService.decide(principal.getId(), id, decision, decisionForm.getDecisionNote());
        redirectAttributes.addFlashAttribute("message",
                decision == RequestStatus.APPROVED ? "Zahtjev je odobren." : "Zahtjev je odbijen.");
        return "redirect:/team/requests";
    }
}
