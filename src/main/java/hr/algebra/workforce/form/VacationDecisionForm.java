package hr.algebra.workforce.form;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class VacationDecisionForm {

    @Size(max = 250, message = "Obrazloženje smije imati najviše 250 znakova.")
    private String decisionNote;
}
