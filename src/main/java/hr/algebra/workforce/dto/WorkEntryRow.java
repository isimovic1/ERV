package hr.algebra.workforce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkEntryRow(Long id,
                           LocalDate workDate,
                           BigDecimal hours,
                           BigDecimal overtimeHours,
                           String description) {
}
