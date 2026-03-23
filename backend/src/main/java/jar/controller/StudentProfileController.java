package jar.controller;

import jar.entity.Attempt;
import jar.entity.CodingSubmission;
import jar.entity.PracticeAttempt;
import jar.entity.StudentBadge;
import jar.entity.User;
import jar.repository.AttemptRepository;
import jar.repository.CodingSubmissionRepository;
import jar.repository.PracticeAttemptRepository;
import jar.repository.StudentBadgeRepository;
import jar.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/profile")
public class StudentProfileController {

    private final UserRepository userRepo;
    private final StudentBadgeRepository badgeRepo;
    private final AttemptRepository attemptRepo;
    private final PracticeAttemptRepository practiceAttemptRepo;
    private final CodingSubmissionRepository codingSubmissionRepo;

    public StudentProfileController(UserRepository userRepo,
                                    StudentBadgeRepository badgeRepo,
                                    AttemptRepository attemptRepo,
                                    PracticeAttemptRepository practiceAttemptRepo,
                                    CodingSubmissionRepository codingSubmissionRepo) {
        this.userRepo = userRepo;
        this.badgeRepo = badgeRepo;
        this.attemptRepo = attemptRepo;
        this.practiceAttemptRepo = practiceAttemptRepo;
        this.codingSubmissionRepo = codingSubmissionRepo;
    }

    @GetMapping
    public Map<String, Object> profile(Authentication auth) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        List<StudentBadge> badges = badgeRepo.findByStudent(student);

        List<PracticeAttempt> practiceAttempts = practiceAttemptRepo.findByStudent(student);
        List<CodingSubmission> codingSubmissions = codingSubmissionRepo.findByStudent(student);
        List<Attempt> attempts = attemptRepo.findByStudent(student);

        double aptitudeAccuracy = practiceAttempts.isEmpty()
                ? 0.0 : (practiceAttempts.stream().filter(PracticeAttempt::isCorrect).count() * 100.0 / practiceAttempts.size());
        long acceptedCoding = codingSubmissions.stream().filter(s -> "ACCEPTED".equalsIgnoreCase(s.getStatus())).count();
        double assessmentAvg = attempts.stream().filter(Attempt::isSubmitted).mapToDouble(Attempt::getScore).average().orElse(0.0);
        double readiness = Math.min(100.0, (aptitudeAccuracy * 0.4) + (acceptedCoding * 2.0) + (assessmentAvg * 5.0));

        Map<String, Object> response = new HashMap<>();
        response.put("id", student.getId());
        response.put("name", student.getName());
        response.put("email", student.getEmail());
        response.put("rollNumber", student.getRollNumber());
        response.put("branch", student.getBranch());
        response.put("batchYear", student.getBatchYear());
        response.put("section", student.getSection());
        response.put("phone", student.getPhone());
        response.put("badges", badges);
        response.put("placementReadinessScore", Math.round(readiness * 100.0) / 100.0);
        return response;
    }

    @PatchMapping
    public Map<String, Object> updateProfile(Authentication auth,
                                             @RequestBody Map<String, String> request) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        if (request.containsKey("name")) {
            student.setName(request.get("name"));
        }
        if (request.containsKey("phone")) {
            String phone = request.get("phone");
            if (phone != null && !phone.isBlank() && !phone.matches("^[0-9]{10}$")) {
                throw new RuntimeException("Phone number must be 10 digits");
            }
            student.setPhone(phone);
        }
        userRepo.save(student);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated");
        response.put("name", student.getName());
        response.put("phone", student.getPhone());
        return response;
    }
}
