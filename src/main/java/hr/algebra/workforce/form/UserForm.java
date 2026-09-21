package hr.algebra.workforce.form;

import hr.algebra.workforce.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UserForm {

    private Long id;

    @NotBlank(message = "Ime je obavezno.")
    @Size(max = 50, message = "Ime smije imati najviše 50 znakova.")
    private String firstName;

    @NotBlank(message = "Prezime je obavezno.")
    @Size(max = 50, message = "Prezime smije imati najviše 50 znakova.")
    private String lastName;

    @NotBlank(message = "E-pošta je obavezna.")
    @Email(message = "Unesite ispravnu adresu e-pošte.")
    @Size(max = 120, message = "E-pošta smije imati najviše 120 znakova.")
    private String email;

    @NotNull(message = "Uloga je obavezna.")
    private Role role;

    private Long managerId;

    @NotNull(message = "Datum zaposlenja je obavezan.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hireDate;

    @NotNull(message = "Broj dana godišnjeg odmora je obavezan.")
    @Min(value = 20, message = "Zakonski minimum je 20 dana godišnjeg odmora.")
    @Max(value = 60, message = "Najviše 60 dana godišnjeg odmora.")
    private Integer vacationDaysPerYear = 20;

    private Boolean active = Boolean.TRUE;

    @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 znaka.")
    private String password;
}
