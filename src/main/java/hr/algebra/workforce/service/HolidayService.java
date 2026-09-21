package hr.algebra.workforce.service;

import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.model.Holiday;
import hr.algebra.workforce.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public Set<LocalDate> holidaysBetween(LocalDate from, LocalDate to) {
        return holidayRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<Holiday> allHolidays() {
        return holidayRepository.findAllByOrderByDateAsc();
    }

    @Transactional
    public int generateForYear(int year) {
        List<Holiday> generated = HolidayCalculator.forYear(year).stream()
                .filter(holiday -> !holidayRepository.existsByDate(holiday.getDate()))
                .toList();
        holidayRepository.saveAll(generated);
        log.info("Generirano {} praznika za {}", generated.size(), year);
        return generated.size();
    }

    @Transactional
    public void add(LocalDate date, String name) {
        if (holidayRepository.existsByDate(date)) {
            throw new BusinessRuleException("Za taj datum praznik već postoji.");
        }
        holidayRepository.save(new Holiday(date, name));
    }

    @Transactional
    public void delete(Long holidayId) {
        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Praznik ne postoji: " + holidayId));
        holidayRepository.delete(holiday);
    }
}
