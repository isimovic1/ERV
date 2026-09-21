package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.WorkEntryForm;
import hr.algebra.workforce.security.AppUserDetails;
import hr.algebra.workforce.service.WorkEntryService;
import hr.algebra.workforce.validation.WorkEntryFormValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.YearMonth;

@Controller
@RequestMapping("/work-entries")
@RequiredArgsConstructor
public class WorkEntryController {

    private final WorkEntryService workEntryService;
    private final WorkEntryFormValidator validator;

    @GetMapping
    public String list(@AuthenticationPrincipal AppUserDetails principal,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                       Model model) {
        if (!model.containsAttribute("workEntryForm")) {
            model.addAttribute("workEntryForm", new WorkEntryForm());
        }
        return renderList(principal, month, model);
    }

    @GetMapping("/{id}/edit")
    public String edit(@AuthenticationPrincipal AppUserDetails principal,
                       @PathVariable Long id,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                       Model model) {
        model.addAttribute("workEntryForm", workEntryService.findForEdit(principal.getId(), id));
        return renderList(principal, month, model);
    }

    @PostMapping
    public String save(@AuthenticationPrincipal AppUserDetails principal,
                       @Valid @ModelAttribute WorkEntryForm workEntryForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        validator.validate(workEntryForm, bindingResult, principal.getId());
        if (bindingResult.hasErrors()) {
            return renderList(principal, monthOf(workEntryForm), model);
        }
        workEntryService.save(principal.getId(), workEntryForm);
        redirectAttributes.addFlashAttribute("message", "Unos je spremljen.");
        return redirectToMonth(monthOf(workEntryForm));
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUserDetails principal,
                         @PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                         RedirectAttributes redirectAttributes) {
        workEntryService.delete(principal.getId(), id);
        redirectAttributes.addFlashAttribute("message", "Unos je obrisan.");
        return redirectToMonth(month);
    }

    private String renderList(AppUserDetails principal, YearMonth month, Model model) {
        YearMonth selected = month == null ? YearMonth.now() : month;
        model.addAttribute("summary", workEntryService.monthlySummary(principal.getId(), selected));
        model.addAttribute("month", selected);
        model.addAttribute("previousMonth", selected.minusMonths(1));
        model.addAttribute("nextMonth", selected.plusMonths(1));
        return "work-entries";
    }

    private YearMonth monthOf(WorkEntryForm form) {
        return form.getWorkDate() == null ? YearMonth.now() : YearMonth.from(form.getWorkDate());
    }

    private String redirectToMonth(YearMonth month) {
        return "redirect:/work-entries?month=" + (month == null ? YearMonth.now() : month);
    }
}
