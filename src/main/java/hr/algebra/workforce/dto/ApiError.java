package hr.algebra.workforce.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(int status,
                       String message,
                       List<String> errors,
                       LocalDateTime timestamp) {

    public static ApiError of(int status, String message, List<String> errors) {
        return new ApiError(status, message, errors, LocalDateTime.now());
    }
}
