package jar.controller;

import jar.entity.Assessment;
import jar.entity.Attempt;
import jar.entity.LeaderboardScore;
import jar.entity.PracticeStreak;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.AttemptRepository;
import jar.repository.LeaderboardScoreRepository;
import jar.repository.PracticeStreakRepository;
import jar.repository.UserRepository;
import jar.service.AssessmentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/home")
public class StudentHomeController {

    private final UserRepository userRepo;
    private final AssessmentService assessmentService;
    private final AssessmentRepository assessmentRepo;
    private final AttemptRepository attemptRepo;
    private final PracticeStreakRepository streakRepo;
    private final LeaderboardScoreRepository leaderboardScoreRepo;

    public StudentHomeController(UserRepository userRepo,
                                 AssessmentService assessmentService,
                                 AssessmentRepository assessmentRepo,
                                 AttemptRepository attemptRepo,
                                 PracticeStreakRepository streakRepo,
                                 LeaderboardScoreRepository leaderboardScoreRepo) {
        this.userRepo = userRepo;
        this.assessmentService = assessmentService;
        this.assessmentRepo = assessmentRepo;
        this.attemptRepo = attemptRepo;
        this.streakRepo = streakRepo;
        this.leaderboardScoreRepo = leaderboardScoreRepo;
    }

    @GetMapping
    public Map<String, Object> home(Authentication auth) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        List<Assessment> visible = assessmentService.getVisibleForStudent(auth.getName());
        List<Attempt> attempts = attemptRepo.findByStudent(student).stream().filter(Attempt::isSubmitted).toList();

        long completedAssessmentIds = attempts.stream()
                .filter(a -> a.getAssessment() != null)
                .map(a -> a.getAssessment().getId())
                .distinct()
                .count();

        PracticeStreak streak = streakRepo.findByStudent(student)
                .orElse(PracticeStreak.builder().currentStreak(0).bestStreak(0).build());
        LeaderboardScore score = leaderboardScoreRepo.findByStudent(student)
                .orElse(LeaderboardScore.builder().totalScore(0).assessmentScore(0).practiceScore(0).codingScore(0).build());

        List<Assessment> upcoming = visible.stream()
                .filter(a -> a.getStartTime() != null && a.getStartTime().isAfter(LocalDateTime.now()))
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .limit(3)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("student", Map.of(
                "name", student.getName(),
                "rollNumber", student.getRollNumber() == null ? "" : student.getRollNumber(),
                "branch", student.getBranch() == null ? "" : student.getBranch(),
                "batchYear", student.getBatchYear() == null ? 0 : student.getBatchYear(),
                "section", student.getSection() == null ? "" : student.getSection()
        ));
        response.put("assignedAssessments", visible.size());
        response.put("completedAssessments", completedAssessmentIds);
        response.put("completionPercentage", visible.isEmpty() ? 0.0 : (completedAssessmentIds * 100.0 / visible.size()));
        response.put("upcoming", upcoming);
        response.put("currentStreak", streak.getCurrentStreak());
        response.put("bestStreak", streak.getBestStreak());
        response.put("leaderboardScore", score.getTotalScore());
        response.put("recentAttempts", attempts.stream().sorted((a, b) -> {
            LocalDateTime aTime = a.getEndTime() == null ? a.getStartTime() : a.getEndTime();
            LocalDateTime bTime = b.getEndTime() == null ? b.getStartTime() : b.getEndTime();
            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;
            return bTime.compareTo(aTime);
        }).limit(5).map(attempt -> {
            Map<String, Object> row = new HashMap<>();
            row.put("assessment", attempt.getAssessment() == null ? "N/A" : safe(attempt.getAssessment().getTitle()));
            row.put("score", attempt.getScore());
            row.put("timestamp", attempt.getEndTime() == null ? attempt.getStartTime() : attempt.getEndTime());
            return row;
        }).toList());
        response.put("totalAssessments", assessmentRepo.count());
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
