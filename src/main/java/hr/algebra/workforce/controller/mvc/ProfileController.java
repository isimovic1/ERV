package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.PasswordChangeForm;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public String profile(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        model.addAttribute("principal", principal);
        return "profile";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal AppUserDetails principal,
                                 @Valid @ModelAttribute PasswordChangeForm passwordChangeForm,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (!passwordChangeForm.getNewPassword().equals(passwordChangeForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Potvrda se ne podudara s novom lozinkom.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("principal", principal);
            return "profile";
        }
        profileService.changePassword(principal.getId(), passwordChangeForm);
        redirectAttributes.addFlashAttribute("message", "Lozinka je promijenjena.");
        return "redirect:/profile";
    }
}
