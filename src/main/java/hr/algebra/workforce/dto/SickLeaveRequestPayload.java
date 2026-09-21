package hr.algebra.workforce.dto;

import hr.algebra.workforce.form.SickLeaveForm;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SickLeaveRequestPayload(@NotNull(message = "Datum početka je obavezan.")
                                      @PastOrPresent(message = "Bolovanje se ne može prijaviti unaprijed.")
                                      LocalDate startDate,

                                      LocalDate endDate,

                                      @Size(max = 250, message = "Opis smije imati najviše 250 znakova.")
                                      String description,

                                      @Size(max = 100, message = "Oznaka doznake smije imati najviše 100 znakova.")
                                      String documentReference) {

    public SickLeaveForm toForm() {
        SickLeaveForm form = new SickLeaveForm();
        form.setStartDate(startDate);
        form.setEndDate(endDate);
        form.setDescription(description);
        form.setDocumentReference(documentReference);
        return form;
    }
}
