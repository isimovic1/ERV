package hr.algebra.workforce.form;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class WorkEntryForm {

    private Long id;

    @NotNull(message = "Datum je obavezan.")
    @PastOrPresent(message = "Datum ne može biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate workDate;

    @NotNull(message = "Broj sati je obavezan.")
    @DecimalMin(value = "0.5", message = "Najmanji unos je 0,5 sati.")
    @DecimalMax(value = "16.0", message = "Najveći unos je 16 sati.")
    private BigDecimal hours;

    @DecimalMin(value = "0.0", message = "Prekovremeni sati ne mogu biti negativni.")
    @DecimalMax(value = "8.0", message = "Najviše 8 prekovremenih sati.")
    private BigDecimal overtimeHours;

    @Size(max = 250, message = "Opis smije imati najviše 250 znakova.")
    private String description;
}
