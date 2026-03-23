package jar.controller;

import jar.entity.Attempt;
import jar.entity.LeaderboardScore;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.AttemptRepository;
import jar.repository.LeaderboardScoreRepository;
import jar.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardStatsController {

    private final UserRepository userRepo;
    private final AssessmentRepository assessmentRepo;
    private final AttemptRepository attemptRepo;
    private final LeaderboardScoreRepository leaderboardScoreRepo;

    public AdminDashboardStatsController(UserRepository userRepo,
                                         AssessmentRepository assessmentRepo,
                                         AttemptRepository attemptRepo,
                                         LeaderboardScoreRepository leaderboardScoreRepo) {
        this.userRepo = userRepo;
        this.assessmentRepo = assessmentRepo;
        this.attemptRepo = attemptRepo;
        this.leaderboardScoreRepo = leaderboardScoreRepo;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<User> students = userRepo.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().name().equals("STUDENT"))
                .toList();
        List<Attempt> attempts = attemptRepo.findAll();

        long activeAssessments = assessmentRepo.findAll().stream()
                .filter(a -> a.getStartTime() != null && a.getEndTime() != null)
                .filter(a -> LocalDateTime.now().isAfter(a.getStartTime()) && LocalDateTime.now().isBefore(a.getEndTime()))
                .count();

        double avgScore = attempts.stream()
                .filter(Attempt::isSubmitted)
                .mapToDouble(Attempt::getScore)
                .average()
                .orElse(0.0);

        long studentsWithZeroAttempts = students.stream()
                .filter(student -> attemptRepo.findByStudent(student).isEmpty())
                .count();

        Map<String, Long> branchBreakdown = students.stream()
                .collect(Collectors.groupingBy(
                        student -> student.getBranch() == null ? "UNKNOWN" : student.getBranch(),
                        Collectors.counting()
                ));

        Map<Integer, Long> batchBreakdown = students.stream()
                .collect(Collectors.groupingBy(
                        student -> student.getBatchYear() == null ? 0 : student.getBatchYear(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> recentActivity = attempts.stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getEndTime() == null ? a.getStartTime() : a.getEndTime();
                    LocalDateTime bTime = b.getEndTime() == null ? b.getStartTime() : b.getEndTime();
                    if (aTime == null && bTime == null) return 0;
                    if (aTime == null) return 1;
                    if (bTime == null) return -1;
                    return bTime.compareTo(aTime);
                })
                .limit(10)
                .map(attempt -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("student", attempt.getStudent() == null ? "N/A" : safe(attempt.getStudent().getName()));
                    item.put("rollNumber", attempt.getStudent() == null ? "N/A" : safe(attempt.getStudent().getRollNumber()));
                    item.put("assessment", attempt.getAssessment() == null ? "N/A" : safe(attempt.getAssessment().getTitle()));
                    item.put("score", attempt.getScore());
                    item.put("timestamp", attempt.getEndTime() == null ? attempt.getStartTime() : attempt.getEndTime());
                    return item;
                })
                .toList();

        List<LeaderboardScore> top = leaderboardScoreRepo.findAllByOrderByTotalScoreDescLastUpdatedAsc().stream()
                .limit(5)
                .toList();
        List<Map<String, Object>> topPerformers = top.stream()
                .map(score -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", score.getStudent() == null ? "N/A" : safe(score.getStudent().getName()));
                    item.put("rollNumber", score.getStudent() == null ? "N/A" : safe(score.getStudent().getRollNumber()));
                    item.put("branch", score.getStudent() == null ? "N/A" : safe(score.getStudent().getBranch()));
                    item.put("totalScore", score.getTotalScore());
                    return item;
                })
                .toList();

        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusDays(14);
        List<Map<String, Object>> lowPerformers = students.stream()
                .filter(student -> attemptRepo.findByStudent(student).stream()
                        .noneMatch(attempt -> attempt.getStartTime() != null && attempt.getStartTime().isAfter(twoWeeksAgo)))
                .map(student -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", safe(student.getName()));
                    item.put("rollNumber", safe(student.getRollNumber()));
                    item.put("branch", safe(student.getBranch()));
                    item.put("batchYear", student.getBatchYear());
                    return item;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("totalStudents", students.size());
        response.put("activeAssessments", activeAssessments);
        response.put("avgScore", avgScore);
        response.put("studentsWithZeroAttempts", studentsWithZeroAttempts);
        response.put("branchBreakdown", branchBreakdown);
        response.put("batchBreakdown", batchBreakdown);
        response.put("recentActivity", recentActivity);
        response.put("topPerformers", topPerformers);
        response.put("lowPerformers", lowPerformers);
        return response;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
