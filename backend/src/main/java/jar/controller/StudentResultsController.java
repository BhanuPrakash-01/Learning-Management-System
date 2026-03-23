package jar.controller;

import jar.entity.Attempt;
import jar.entity.PracticeAttempt;
import jar.entity.User;
import jar.repository.AttemptRepository;
import jar.repository.PracticeAttemptRepository;
import jar.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/results")
public class StudentResultsController {

    private final UserRepository userRepo;
    private final AttemptRepository attemptRepo;
    private final PracticeAttemptRepository practiceAttemptRepo;

    public StudentResultsController(UserRepository userRepo,
                                    AttemptRepository attemptRepo,
                                    PracticeAttemptRepository practiceAttemptRepo) {
        this.userRepo = userRepo;
        this.attemptRepo = attemptRepo;
        this.practiceAttemptRepo = practiceAttemptRepo;
    }

    @GetMapping
    public Map<String, Object> results(Authentication auth) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        List<Attempt> attempts = attemptRepo.findByStudent(student).stream()
                .filter(Attempt::isSubmitted)
                .toList();
        List<PracticeAttempt> practiceAttempts = practiceAttemptRepo.findByStudent(student);

        double avg = attempts.stream().mapToDouble(Attempt::getScore).average().orElse(0.0);
        double best = attempts.stream().mapToDouble(Attempt::getScore).max().orElse(0.0);
        double worst = attempts.stream().mapToDouble(Attempt::getScore).min().orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("summary", Map.of(
                "totalAttempts", attempts.size(),
                "averageScore", avg,
                "bestScore", best,
                "worstScore", worst,
                "totalPracticeQuestions", practiceAttempts.size()
        ));
        response.put("attempts", attempts);
        response.put("scoreTrend", attempts.stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("date", item.getEndTime() == null ? item.getStartTime() : item.getEndTime());
            row.put("score", item.getScore());
            row.put("assessment", item.getAssessment() == null ? "N/A" : safe(item.getAssessment().getTitle()));
            return row;
        }).toList());
        response.put("practiceHistory", practiceAttempts.stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("topic", item.getQuestion() == null || item.getQuestion().getTopic() == null
                    ? "Unknown" : safe(item.getQuestion().getTopic().getName()));
            row.put("correct", item.isCorrect());
            row.put("attemptedAt", item.getAttemptedAt());
            return row;
        }).toList());
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
