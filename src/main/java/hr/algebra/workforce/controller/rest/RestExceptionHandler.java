package hr.algebra.workforce.controller.rest;

import hr.algebra.workforce.dto.ApiError;
import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice(basePackages = "hr.algebra.workforce.controller.rest")
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBeanValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();
        return ApiError.of(HttpStatus.BAD_REQUEST.value(), "Zahtjev nije valjan.", messages);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleDomainValidation(ValidationException exception) {
        return ApiError.of(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), exception.getErrors());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(ResourceNotFoundException exception) {
        log.warn("Nedostupan zapis: {}", exception.getMessage());
        return ApiError.of(HttpStatus.NOT_FOUND.value(), "Traženi zapis ne postoji ili vam nije dostupan.",
                List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleBusinessRule(BusinessRuleException exception) {
        return ApiError.of(HttpStatus.CONFLICT.value(), exception.getMessage(), List.of());
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleAuthentication(Exception exception) {
        log.debug("Neuspjela prijava: {}", exception.getMessage());
        return ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Neispravna e-pošta ili lozinka.", List.of());
    }
}
