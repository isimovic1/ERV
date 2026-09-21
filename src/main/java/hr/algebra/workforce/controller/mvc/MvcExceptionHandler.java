package hr.algebra.workforce.controller.mvc;

import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackages = "hr.algebra.workforce.controller.mvc")
@Slf4j
public class MvcExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException exception, Model model) {
        log.warn("Nedostupan zapis: {}", exception.getMessage());
        model.addAttribute("errorMessage", "Traženi zapis ne postoji ili vam nije dostupan.");
        return "error/not-found";
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleBusinessRule(BusinessRuleException exception, Model model) {
        log.warn("Prekršeno poslovno pravilo: {}", exception.getMessage());
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/not-found";
    }
}
