package hr.algebra.workforce.dto;

import java.math.BigDecimal;

public record MonthlyReportRow(Long employeeId,
                               String employeeName,
                               BigDecimal workedHours,
                               BigDecimal overtimeHours,
                               int workedDays,
                               int vacationDays,
                               int sickDays) {
}
