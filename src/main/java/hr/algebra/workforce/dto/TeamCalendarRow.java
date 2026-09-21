package hr.algebra.workforce.dto;

import java.util.List;

public record TeamCalendarRow(Long employeeId,
                              String employeeName,
                              List<CalendarDay> days,
                              int absentDays) {
}
