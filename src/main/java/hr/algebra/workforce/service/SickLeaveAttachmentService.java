package hr.algebra.workforce.service;

import hr.algebra.workforce.exception.BusinessRuleException;
import hr.algebra.workforce.exception.ResourceNotFoundException;
import hr.algebra.workforce.model.Role;
import hr.algebra.workforce.model.SickLeave;
import hr.algebra.workforce.model.SickLeaveAttachment;
import hr.algebra.workforce.repository.SickLeaveAttachmentRepository;
import hr.algebra.workforce.repository.SickLeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class SickLeaveAttachmentService {

    private final SickLeaveAttachmentRepository attachmentRepository;
    private final SickLeaveRepository sickLeaveRepository;

    @Transactional
    public void store(SickLeave sickLeave, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        if (!AttachmentPolicy.isAllowedType(file.getContentType())) {
            throw new BusinessRuleException("Dopuštene su samo datoteke tipa "
                    + AttachmentPolicy.describeAllowedTypes() + ".");
        }
        if (file.getSize() > AttachmentPolicy.MAX_SIZE_BYTES) {
            throw new BusinessRuleException("Datoteka smije biti najviše 5 MB.");
        }
        SickLeaveAttachment attachment = new SickLeaveAttachment();
        attachment.setSickLeave(sickLeave);
        attachment.setFileName(safeFileName(file.getOriginalFilename()));
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setContent(readBytes(file));
        attachmentRepository.save(attachment);
        log.debug("Spremljen prilog {} uz bolovanje {}", attachment.getFileName(), sickLeave.getId());
    }

    @Transactional(readOnly = true)
    public SickLeaveAttachment download(Long viewerId, Role viewerRole, Long sickLeaveId) {
        SickLeave leave = sickLeaveRepository.findById(sickLeaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Bolovanje ne postoji: " + sickLeaveId));
        requireAccess(viewerId, viewerRole, leave);
        return attachmentRepository.findBySickLeaveId(sickLeaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Prilog ne postoji za bolovanje: " + sickLeaveId));
    }

    @Transactional(readOnly = true)
    public boolean hasAttachment(Long sickLeaveId) {
        return attachmentRepository.existsBySickLeaveId(sickLeaveId);
    }

    private void requireAccess(Long viewerId, Role viewerRole, SickLeave leave) {
        if (viewerRole == Role.ADMIN || leave.getUser().getId().equals(viewerId)) {
            return;
        }
        boolean isOwnManager = leave.getUser().getManager() != null
                && leave.getUser().getManager().getId().equals(viewerId);
        if (!isOwnManager) {
            throw new ResourceNotFoundException("Prilog nije dostupan: " + leave.getId());
        }
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "prilog";
        }
        String name = Paths.get(originalName).getFileName().toString();
        return name.length() > 150 ? name.substring(name.length() - 150) : name;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Čitanje priložene datoteke nije uspjelo", exception);
        }
    }
}
