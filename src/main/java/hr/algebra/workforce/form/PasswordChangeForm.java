package hr.algebra.workforce.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordChangeForm {

    @NotBlank(message = "Trenutna lozinka je obavezna.")
    private String currentPassword;

    @NotBlank(message = "Nova lozinka je obavezna.")
    @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 znaka.")
    private String newPassword;

    @NotBlank(message = "Potvrda lozinke je obavezna.")
    private String confirmPassword;
}
