package hr.algebra.workforce.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Zahtjev nije valjan.");
        this.errors = errors;
    }
}
