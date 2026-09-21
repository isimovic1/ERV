package hr.algebra.workforce.dto;

import java.time.LocalDate;

public record CalendarDay(LocalDate date, AbsenceKind kind) {
}
