package hr.algebra.workforce.dto;

import hr.algebra.workforce.form.WorkEntryForm;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkEntryRequest(@NotNull(message = "Datum je obavezan.")
                               @PastOrPresent(message = "Datum ne može biti u budućnosti.")
                               LocalDate workDate,

                               @NotNull(message = "Broj sati je obavezan.")
                               @DecimalMin(value = "0.5", message = "Najmanji unos je 0,5 sati.")
                               @DecimalMax(value = "16.0", message = "Najveći unos je 16 sati.")
                               BigDecimal hours,

                               @DecimalMin(value = "0.0", message = "Prekovremeni sati ne mogu biti negativni.")
                               @DecimalMax(value = "8.0", message = "Najviše 8 prekovremenih sati.")
                               BigDecimal overtimeHours,

                               @Size(max = 250, message = "Opis smije imati najviše 250 znakova.")
                               String description) {

    public WorkEntryForm toForm() {
        WorkEntryForm form = new WorkEntryForm();
        form.setWorkDate(workDate);
        form.setHours(hours);
        form.setOvertimeHours(overtimeHours);
        form.setDescription(description);
        return form;
    }
}
