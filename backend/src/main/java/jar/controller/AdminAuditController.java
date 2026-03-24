package jar.controller;

import jar.repository.AdminAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AdminAuditLogRepository auditLogRepository;

    public AdminAuditController(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public Map<String, Object> logs(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "25") int size) {
        var result = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").descending())
        );
        return Map.of(
                "content", result.getContent(),
                "page", page,
                "size", size,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }
}
