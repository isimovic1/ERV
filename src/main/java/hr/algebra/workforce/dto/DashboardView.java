package hr.algebra.workforce.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record DashboardView(String fullName,
                            YearMonth month,
                            BigDecimal monthHours,
                            BigDecimal monthOvertimeHours,
                            int recordedDays,
                            int vacationRemaining,
                            int vacationUsed,
                            int vacationPending,
                            boolean onSickLeave,
                            boolean todayEntryMissing,
                            int pendingTeamRequests) {
}
