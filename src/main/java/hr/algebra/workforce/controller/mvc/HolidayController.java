package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.form.HolidayForm;
import hr.algebra.workforce.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    public String list(Model model) {
        if (!model.containsAttribute("holidayForm")) {
            model.addAttribute("holidayForm", new HolidayForm());
        }
        return render(model);
    }

    @PostMapping
    public String add(@Valid @ModelAttribute HolidayForm holidayForm,
                      BindingResult bindingResult,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return render(model);
        }
        holidayService.add(holidayForm.getDate(), holidayForm.getName().trim());
        redirectAttributes.addFlashAttribute("message", "Praznik je dodan.");
        return "redirect:/admin/holidays";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam int year, RedirectAttributes redirectAttributes) {
        int created = holidayService.generateForYear(year);
        redirectAttributes.addFlashAttribute("message",
                created == 0 ? "Praznici za " + year + ". već postoje."
                        : "Dodano " + created + " praznika za " + year + ".");
        return "redirect:/admin/holidays";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        holidayService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Praznik je obrisan.");
        return "redirect:/admin/holidays";
    }

    private String render(Model model) {
        model.addAttribute("holidays", holidayService.allHolidays());
        model.addAttribute("nextYear", LocalDate.now().getYear() + 1);
        return "admin-holidays";
    }
}
