package hr.algebra.workforce.service;

import hr.algebra.workforce.model.Holiday;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HolidayCalculator {

    private static final Map<MonthDay, String> FIXED_HOLIDAYS = Map.ofEntries(
            Map.entry(MonthDay.of(1, 1), "Nova godina"),
            Map.entry(MonthDay.of(1, 6), "Bogojavljenje"),
            Map.entry(MonthDay.of(5, 1), "Praznik rada"),
            Map.entry(MonthDay.of(5, 30), "Dan državnosti"),
            Map.entry(MonthDay.of(6, 22), "Dan antifašističke borbe"),
            Map.entry(MonthDay.of(8, 5), "Dan pobjede i domovinske zahvalnosti"),
            Map.entry(MonthDay.of(8, 15), "Velika Gospa"),
            Map.entry(MonthDay.of(11, 1), "Svi sveti"),
            Map.entry(MonthDay.of(11, 18), "Dan sjećanja na žrtve Domovinskog rata"),
            Map.entry(MonthDay.of(12, 25), "Božić"),
            Map.entry(MonthDay.of(12, 26), "Sveti Stjepan"));

    private HolidayCalculator() {
    }

    public static List<Holiday> forYear(int year) {
        List<Holiday> holidays = new ArrayList<>();
        FIXED_HOLIDAYS.forEach((monthDay, name) -> holidays.add(new Holiday(monthDay.atYear(year), name)));
        LocalDate easter = easterSunday(year);
        holidays.add(new Holiday(easter, "Uskrs"));
        holidays.add(new Holiday(easter.plusDays(1), "Uskrsni ponedjeljak"));
        holidays.add(new Holiday(easter.plusDays(60), "Tijelovo"));
        holidays.sort((first, second) -> first.getDate().compareTo(second.getDate()));
        return holidays;
    }

    public static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
