package jar.controller;

import jar.entity.Attempt;
import jar.entity.User;
import jar.repository.AttemptRepository;
import jar.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public AdminStudentsController(UserRepository userRepo,
                                   AttemptRepository attemptRepo,
                                   PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.attemptRepo = attemptRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Map<String, Object> getStudents(@RequestParam(required = false, defaultValue = "") String search,
                                           @RequestParam(required = false) String branch,
                                           @RequestParam(required = false) Integer batchYear,
                                           @RequestParam(required = false) String section,
                                           @RequestParam(required = false, defaultValue = "0") int page,
                                           @RequestParam(required = false, defaultValue = "20") int size) {
        List<User> matched = userRepo.searchStudents(search, branch, batchYear, section).stream()
                .filter(user -> user.getRole() != null && user.getRole().name().equals("STUDENT"))
                .toList();

        int fromIndex = Math.min(page * size, matched.size());
        int toIndex = Math.min(fromIndex + size, matched.size());
        List<User> pageData = matched.subList(fromIndex, toIndex);

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
        response.put("totalElements", matched.size());
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
    public ResponseEntity<?> deactivateStudent(@PathVariable Long id) {
        User student = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        student.setActive(false);
        userRepo.save(student);
        return ResponseEntity.ok(Map.of("message", "Student deactivated"));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        User student = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
        student.setPassword(passwordEncoder.encode("Anurag@123"));
        userRepo.save(student);
        return ResponseEntity.ok(Map.of("message", "Password reset to default"));
    }
}
