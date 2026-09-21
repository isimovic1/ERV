package hr.algebra.workforce.service;

import hr.algebra.workforce.AbstractIntegrationTest;
import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.model.Holiday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HolidayServiceTest extends AbstractIntegrationTest {

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        holidayRepository.deleteAll();
    }

    @Test
    void easterIsCalculatedForKnownYears() {
        assertThat(HolidayCalculator.easterSunday(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(HolidayCalculator.easterSunday(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
        assertThat(HolidayCalculator.easterSunday(2025)).isEqualTo(LocalDate.of(2025, 4, 20));
    }

    @Test
    void generatedYearContainsFixedAndMovableHolidays() {
        int created = holidayService.generateForYear(2026);

        assertThat(created).isEqualTo(14);
        List<Holiday> holidays = holidayService.allHolidays();
        assertThat(holidays).extracting(Holiday::getDate)
                .contains(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 30), LocalDate.of(2026, 12, 25),
                        LocalDate.of(2026, 4, 6), LocalDate.of(2026, 6, 4));
        assertThat(holidays).extracting(Holiday::getName).contains("Uskrsni ponedjeljak", "Tijelovo");
    }

    @Test
    void generatingSameYearTwiceAddsNothing() {
        holidayService.generateForYear(2026);

        assertThat(holidayService.generateForYear(2026)).isZero();
        assertThat(holidayService.allHolidays()).hasSize(14);
    }

    @Test
    void duplicateHolidayIsRejected() {
        holidayService.add(LocalDate.of(2026, 7, 7), "Dan tvrtke");

        assertThatThrownBy(() -> holidayService.add(LocalDate.of(2026, 7, 7), "Nešto drugo"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void workingDaysSkipHolidaysInsideRange() {
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)))
                .isEqualTo(5);

        holidayService.generateForYear(2026);

        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 7)))
                .isEqualTo(4);
    }

    @Test
    void holidayFallingOnWeekendDoesNotReduceWorkingDays() {
        holidayService.generateForYear(2026);

        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16)))
                .isEqualTo(5);
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 31)))
                .isEqualTo(5);
    }

    @Test
    void easterMondayAndCorpusChristiReduceWorkingDays() {
        holidayService.generateForYear(2026);

        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10)))
                .isEqualTo(4);
        assertThat(leaveService.workingDaysBetween(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5)))
                .isEqualTo(4);
    }

}
