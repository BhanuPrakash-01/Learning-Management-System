package jar.controller;

import jar.entity.Attempt;
import jar.entity.Role;
import jar.entity.User;
import jar.repository.AttemptRepository;
import jar.repository.UserRepository;
import jar.service.auth.AccountSecurityService;
import jar.service.auth.AdminBootstrapService;
import jar.service.security.AdminAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentsController {

    private final UserRepository userRepo;
    private final AttemptRepository attemptRepo;
    private final AccountSecurityService accountSecurityService;
    private final AdminAuditService adminAuditService;
    private final AdminBootstrapService adminBootstrapService;

    public AdminStudentsController(UserRepository userRepo,
                                   AttemptRepository attemptRepo,
                                   AccountSecurityService accountSecurityService,
                                   AdminAuditService adminAuditService,
                                   AdminBootstrapService adminBootstrapService) {
        this.userRepo = userRepo;
        this.attemptRepo = attemptRepo;
        this.accountSecurityService = accountSecurityService;
        this.adminAuditService = adminAuditService;
        this.adminBootstrapService = adminBootstrapService;
    }

    @GetMapping
    public Map<String, Object> getStudents(@RequestParam(required = false, defaultValue = "") String search,
                                           @RequestParam(required = false) String branch,
                                           @RequestParam(required = false) Integer batchYear,
                                           @RequestParam(required = false) String section,
                                           @RequestParam(required = false, defaultValue = "0") int page,
                                           @RequestParam(required = false, defaultValue = "20") int size) {
        Page<User> matchedPage = userRepo.searchUsersByRole(
                Role.STUDENT,
                search,
                branch,
                batchYear,
                section,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("name").ascending())
        );
        List<User> pageData = matchedPage.getContent();

        List<Map<String, Object>> rows = pageData.stream().map(student -> {
            List<Attempt> attempts = attemptRepo.findByStudent(student);
            long attemptsTaken = attempts.stream().filter(Attempt::isSubmitted).count();
            double avgScore = attempts.stream().filter(Attempt::isSubmitted).mapToDouble(Attempt::getScore).average().orElse(0.0);
            LocalDateTime lastActive = attempts.stream()
                    .map(a -> a.getEndTime() == null ? a.getStartTime() : a.getEndTime())
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            Map<String, Object> row = new HashMap<>();
            row.put("id", student.getId());
            row.put("rollNumber", student.getRollNumber());
            row.put("name", student.getName());
            row.put("branch", student.getBranch());
            row.put("batchYear", student.getBatchYear());
            row.put("section", student.getSection());
            row.put("assessmentsTaken", attemptsTaken);
            row.put("avgScore", avgScore);
            row.put("lastActive", lastActive);
            row.put("active", student.getActive());
            return row;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", rows);
        response.put("totalElements", matchedPage.getTotalElements());
        response.put("totalPages", matchedPage.getTotalPages());
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getStudentProfile(@PathVariable Long id) {
        User student = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        List<Attempt> attempts = attemptRepo.findByStudent(student);
        Map<String, Object> response = new HashMap<>();
        response.put("student", student);
        response.put("attempts", attempts);
        return response;
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateStudent(@PathVariable Long id, Authentication auth) {
        User student = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        student.setActive(false);
        userRepo.save(student);
        adminAuditService.log(auth.getName(), "DEACTIVATE_STUDENT", "USER", String.valueOf(id),
                "Deactivated student " + student.getEmail());
        return ResponseEntity.ok(Map.of("message", "Student deactivated"));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, Authentication auth) {
        User student = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        accountSecurityService.adminResetPassword(student);
        adminAuditService.log(auth.getName(), "RESET_PASSWORD", "USER", String.valueOf(id),
                "Password reset for " + student.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Password reset completed. A temporary password was emailed to the student."
        ));
    }

    @PatchMapping("/{id}/promote-admin")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long id, Authentication auth) {
        if (!adminBootstrapService.hasAdmin()) {
            throw new RuntimeException("Cannot promote admins before bootstrap is complete");
        }
        User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user = adminBootstrapService.promoteToAdmin(user);
        adminAuditService.log(auth.getName(), "PROMOTE_ADMIN", "USER", String.valueOf(id),
                "Promoted user to admin: " + user.getEmail());
        return ResponseEntity.ok(Map.of("message", "User promoted to admin", "email", user.getEmail()));
    }
}
