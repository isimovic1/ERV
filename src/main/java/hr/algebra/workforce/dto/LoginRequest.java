package hr.algebra.workforce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "E-pošta je obavezna.")
                           @Email(message = "Unesite ispravnu adresu e-pošte.")
                           String email,

                           @NotBlank(message = "Lozinka je obavezna.")
                           String password) {
}
