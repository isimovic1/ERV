package hr.algebra.workforce.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlyReport(YearMonth month,
                            List<MonthlyReportRow> rows,
                            BigDecimal totalHours,
                            BigDecimal totalOvertimeHours,
                            int totalVacationDays,
                            int totalSickDays) {

    public static MonthlyReport of(YearMonth month, List<MonthlyReportRow> rows) {
        return new MonthlyReport(month, rows,
                rows.stream().map(MonthlyReportRow::workedHours).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(MonthlyReportRow::overtimeHours).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().mapToInt(MonthlyReportRow::vacationDays).sum(),
                rows.stream().mapToInt(MonthlyReportRow::sickDays).sum());
    }
}
