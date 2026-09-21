package hr.algebra.workforce.configuration;

import hr.algebra.workforce.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Order(1)
@RequiredArgsConstructor
public class HolidayInitializer implements CommandLineRunner {

    private final HolidayService holidayService;

    @Override
    public void run(String... args) {
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear; year <= currentYear + 1; year++) {
            holidayService.generateForYear(year);
        }
    }
}
