package hr.algebra.workforce.service;

import hr.algebra.workforce.dto.MonthlyWorkSummary;
import hr.algebra.workforce.dto.WorkEntryRow;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.form.WorkEntryForm;
import hr.algebra.workforce.model.User;
import hr.algebra.workforce.model.WorkEntry;
import hr.algebra.workforce.repository.UserRepository;
import hr.algebra.workforce.repository.WorkEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkEntryService {

    private final WorkEntryRepository workEntryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MonthlyWorkSummary monthlySummary(Long userId, YearMonth month) {
        List<WorkEntry> entries = workEntryRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, month.atDay(1), month.atEndOfMonth());
        List<WorkEntryRow> rows = entries.stream().map(this::toRow).toList();
        return new MonthlyWorkSummary(month, rows, sum(entries, WorkEntry::getHours),
                sum(entries, WorkEntry::getOvertimeHours), rows.size());
    }

    @Transactional(readOnly = true)
    public boolean existsForDate(Long userId, LocalDate date, Long excludedEntryId) {
        return workEntryRepository.findByUserIdAndWorkDate(userId, date)
                .filter(entry -> !entry.getId().equals(excludedEntryId))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean hasEntriesBetween(Long userId, LocalDate from, LocalDate to) {
        return !workEntryRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from, to).isEmpty();
    }

    @Transactional(readOnly = true)
    public WorkEntryForm findForEdit(Long userId, Long entryId) {
        WorkEntry entry = requireOwnedEntry(userId, entryId);
        WorkEntryForm form = new WorkEntryForm();
        form.setId(entry.getId());
        form.setWorkDate(entry.getWorkDate());
        form.setHours(entry.getHours());
        form.setOvertimeHours(entry.getOvertimeHours());
        form.setDescription(entry.getDescription());
        return form;
    }

    @Transactional
    public void save(Long userId, WorkEntryForm form) {
        WorkEntry entry = form.getId() == null ? newEntry(userId) : requireOwnedEntry(userId, form.getId());
        entry.setWorkDate(form.getWorkDate());
        entry.setHours(form.getHours());
        entry.setOvertimeHours(form.getOvertimeHours() == null ? BigDecimal.ZERO : form.getOvertimeHours());
        entry.setDescription(form.getDescription());
        workEntryRepository.save(entry);
        log.debug("Spremljen unos rada {} za korisnika {}", entry.getWorkDate(), userId);
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        workEntryRepository.delete(requireOwnedEntry(userId, entryId));
    }

    private WorkEntry newEntry(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik ne postoji: " + userId));
        WorkEntry entry = new WorkEntry();
        entry.setUser(user);
        return entry;
    }

    private WorkEntry requireOwnedEntry(Long userId, Long entryId) {
        return workEntryRepository.findById(entryId)
                .filter(entry -> entry.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Unos rada ne postoji: " + entryId));
    }

    private WorkEntryRow toRow(WorkEntry entry) {
        return new WorkEntryRow(entry.getId(), entry.getWorkDate(), entry.getHours(),
                entry.getOvertimeHours(), entry.getDescription());
    }

    private BigDecimal sum(List<WorkEntry> entries, java.util.function.Function<WorkEntry, BigDecimal> extractor) {
        return entries.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
