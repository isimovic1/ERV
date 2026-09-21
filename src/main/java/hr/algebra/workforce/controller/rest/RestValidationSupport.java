package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.exception.ValidationException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.function.BiConsumer;

@Component
public class RestValidationSupport {

    public <T> void validate(T target, BiConsumer<T, Errors> validator) {
        Errors errors = new BeanPropertyBindingResult(target, "payload");
        validator.accept(target, errors);
        if (errors.hasErrors()) {
            throw new ValidationException(errors.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .toList());
        }
    }

    public List<String> messagesOf(Errors errors) {
        return errors.getAllErrors().stream().map(ObjectError::getDefaultMessage).toList();
    }
}
