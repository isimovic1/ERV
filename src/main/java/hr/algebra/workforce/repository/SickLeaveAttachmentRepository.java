package hr.algebra.workforce.repository;

import hr.algebra.workforce.model.SickLeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SickLeaveAttachmentRepository extends JpaRepository<SickLeaveAttachment, Long> {

    Optional<SickLeaveAttachment> findBySickLeaveId(Long sickLeaveId);

    boolean existsBySickLeaveId(Long sickLeaveId);

    void deleteBySickLeaveId(Long sickLeaveId);
}
