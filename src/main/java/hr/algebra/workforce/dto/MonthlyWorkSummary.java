package hr.algebra.workforce.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyWorkSummary(YearMonth month,
                                 List<WorkEntryRow> entries,
                                 BigDecimal totalHours,
                                 BigDecimal totalOvertimeHours,
                                 int recordedDays) {
}
