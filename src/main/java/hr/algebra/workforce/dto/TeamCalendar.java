package hr.algebra.workforce.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record TeamCalendar(YearMonth month,
                           List<LocalDate> days,
                           List<TeamCalendarRow> rows) {
}
