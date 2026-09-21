package hr.algebra.workforce.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class SickLeaveForm {

    @NotNull(message = "Datum početka je obavezan.")
    @PastOrPresent(message = "Bolovanje se ne može prijaviti unaprijed.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 250, message = "Opis smije imati najviše 250 znakova.")
    private String description;

    @Size(max = 100, message = "Oznaka doznake smije imati najviše 100 znakova.")
    private String documentReference;

    private MultipartFile attachment;
}
