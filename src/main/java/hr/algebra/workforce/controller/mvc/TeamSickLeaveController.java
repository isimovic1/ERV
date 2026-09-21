package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.model.SickLeaveAttachment;
import hr.algebra.workforce.service.SickLeaveAttachmentService;
import hr.algebra.workforce.service.SickLeaveReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/team/sick-leaves")
@RequiredArgsConstructor
public class TeamSickLeaveController {

    private final SickLeaveReviewService sickLeaveReviewService;
    private final SickLeaveAttachmentService attachmentService;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails principal, Model model) {
        model.addAttribute("leaves", sickLeaveReviewService.teamLeaves(principal.getId()));
        return "team-sick-leaves";
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

    @PostMapping("/{id}/confirm")
    public String confirm(@AuthenticationPrincipal AppUserDetails principal,
                          @PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        sickLeaveReviewService.confirm(principal.getId(), id);
        redirectAttributes.addFlashAttribute("message", "Bolovanje je potvrđeno.");
        return "redirect:/team/sick-leaves";
    }
}
