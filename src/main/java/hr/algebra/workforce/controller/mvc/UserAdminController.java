package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.UserForm;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.UserAdminService;
import hr.algebra.workforce.validation.UserFormValidator;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final UserFormValidator validator;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userAdminService.allUsers());
        return "admin-users";
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("userForm", new UserForm());
        return renderForm(model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("userForm", userAdminService.findForEdit(id));
        return renderForm(model);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute UserForm userForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        validator.validate(userForm, bindingResult);
        if (bindingResult.hasErrors()) {
            return renderForm(model);
        }
        userAdminService.save(userForm);
        redirectAttributes.addFlashAttribute("message", "Korisnik je spremljen.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/active")
    public String setActive(@AuthenticationPrincipal AppUserDetails principal,
                            @PathVariable Long id,
                            @RequestParam boolean active,
                            RedirectAttributes redirectAttributes) {
        userAdminService.setActive(principal.getId(), id, active);
        redirectAttributes.addFlashAttribute("message",
                active ? "Korisnik je aktiviran." : "Korisnik je deaktiviran.");
        return "redirect:/admin/users";
    }

    private String renderForm(Model model) {
        model.addAttribute("managers", userAdminService.availableManagers());
        model.addAttribute("roles", Role.values());
        return "admin-user-form";
    }
}
