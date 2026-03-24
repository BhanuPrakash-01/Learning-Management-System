package jar.service.security;

import jar.entity.AdminAuditLog;
import jar.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    public AdminAuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String adminEmail, String action, String targetType, String targetId, String details) {
        if (adminEmail == null || adminEmail.isBlank()) {
            return;
        }
        auditLogRepository.save(AdminAuditLog.builder()
                .adminEmail(adminEmail)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
