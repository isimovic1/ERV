package hr.algebra.workforce.form;

import hr.algebra.workforce.model.LeaveType;
import jakarta.validation.constraints.FutureOrPresent;
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
public class LeaveRequestForm {

    @NotNull(message = "Vrsta odsutnosti je obavezna.")
    private LeaveType type = LeaveType.ANNUAL_LEAVE;

    @NotNull(message = "Datum početka je obavezan.")
    @FutureOrPresent(message = "Godišnji odmor ne može započeti u prošlosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "Datum završetka je obavezan.")
    @FutureOrPresent(message = "Datum završetka ne može biti u prošlosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @Size(max = 250, message = "Napomena smije imati najviše 250 znakova.")
    private String note;
}
