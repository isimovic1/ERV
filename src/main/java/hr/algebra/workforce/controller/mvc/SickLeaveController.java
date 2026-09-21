package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.SickLeaveCloseForm;
import hr.algebra.workforce.form.SickLeaveForm;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.model.SickLeaveAttachment;
import hr.algebra.workforce.service.SickLeaveAttachmentService;
import hr.algebra.workforce.service.SickLeaveService;
import hr.algebra.workforce.validation.SickLeaveFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@Controller
@RequestMapping("/sick-leaves")
@RequiredArgsConstructor
public class SickLeaveController {

    private final SickLeaveService sickLeaveService;
    private final SickLeaveFormValidator validator;
    private final SickLeaveAttachmentService attachmentService;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        if (!model.containsAttribute("sickLeaveForm")) {
            model.addAttribute("sickLeaveForm", new SickLeaveForm());
        }
        return render(principal, model);
    }

    @PostMapping
    public String report(@AuthenticationPrincipal AppUserDetails principal,
                         @Valid @ModelAttribute SickLeaveForm sickLeaveForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        validator.validate(sickLeaveForm, bindingResult, principal.getId());
        if (bindingResult.hasErrors()) {
            return render(principal, model);
        }
        sickLeaveService.report(principal.getId(), sickLeaveForm);
        redirectAttributes.addFlashAttribute("message", "Bolovanje je prijavljeno.");
        return "redirect:/sick-leaves";
    }

    @PostMapping("/{id}/close")
    public String close(@AuthenticationPrincipal AppUserDetails principal,
                        @PathVariable Long id,
                        @Valid @ModelAttribute SickLeaveCloseForm sickLeaveCloseForm,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unesite ispravan datum završetka.");
            return "redirect:/sick-leaves";
        }
        sickLeaveService.close(principal.getId(), id, sickLeaveCloseForm);
        redirectAttributes.addFlashAttribute("message", "Bolovanje je zatvoreno.");
        return "redirect:/sick-leaves";
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> attachment(@AuthenticationPrincipal AppUserDetails principal,
                                               @PathVariable Long id) {
        SickLeaveAttachment attachment = attachmentService.download(principal.getId(), principal.getRole(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .contentLength(attachment.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(new ByteArrayResource(attachment.getContent()));
    }

    private String render(AppUserDetails principal, Model model) {
        model.addAttribute("leaves", sickLeaveService.myLeaves(principal.getId()));
        model.addAttribute("sickLeaveCloseForm", new SickLeaveCloseForm());
        return "sick-leaves";
    }
}
