package hr.algebra.workforce.dto;

import hr.algebra.workforce.form.LeaveRequestForm;
import hr.algebra.workforce.model.LeaveType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestPayload(LeaveType type,

                                     @NotNull(message = "Datum početka je obavezan.")
                                     @FutureOrPresent(message = "Godišnji odmor ne može započeti u prošlosti.")
                                     LocalDate startDate,

                                     @NotNull(message = "Datum završetka je obavezan.")
                                     @FutureOrPresent(message = "Datum završetka ne može biti u prošlosti.")
                                     LocalDate endDate,

                                     @Size(max = 250, message = "Napomena smije imati najviše 250 znakova.")
                                     String note) {

    public LeaveRequestForm toForm() {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setType(type == null ? LeaveType.ANNUAL_LEAVE : type);
        form.setStartDate(startDate);
        form.setEndDate(endDate);
        form.setNote(note);
        return form;
    }
}
