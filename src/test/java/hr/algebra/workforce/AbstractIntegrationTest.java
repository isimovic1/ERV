package hr.algebra.workforce;

import hr.algebra.workforce.model.User;
import hr.algebra.workforce.repository.HolidayRepository;
import hr.algebra.workforce.repository.SickLeaveAttachmentRepository;
import hr.algebra.workforce.repository.SickLeaveRepository;
import hr.algebra.workforce.repository.UserRepository;
import hr.algebra.workforce.repository.LeaveRequestRepository;
import hr.algebra.workforce.repository.WorkEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected WorkEntryRepository workEntryRepository;

    @Autowired
    protected LeaveRequestRepository leaveRequestRepository;

    @Autowired
    protected SickLeaveRepository sickLeaveRepository;

    @Autowired
    protected SickLeaveAttachmentRepository sickLeaveAttachmentRepository;

    @Autowired
    protected HolidayRepository holidayRepository;

    @Autowired
    protected UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        workEntryRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        sickLeaveAttachmentRepository.deleteAll();
        sickLeaveRepository.deleteAll();
        holidayRepository.deleteAll();
        List<User> users = userRepository.findAll();
        users.forEach(user -> user.setManager(null));
        userRepository.saveAll(users);
        userRepository.deleteAll();
    }
}
