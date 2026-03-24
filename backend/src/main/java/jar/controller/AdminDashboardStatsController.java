package jar.controller;

import jar.entity.Attempt;
import jar.entity.LeaderboardScore;
import jar.entity.Role;
import jar.repository.AssessmentRepository;
import jar.repository.AttemptRepository;
import jar.repository.LeaderboardScoreRepository;
import jar.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public Map<String, Object> getStats(@RequestParam(defaultValue = "10") int recentLimit,
                                        @RequestParam(defaultValue = "100") int lowPerformerLimit) {
        int safeRecentLimit = Math.max(1, Math.min(recentLimit, 100));
        int safeLowPerformerLimit = Math.max(1, Math.min(lowPerformerLimit, 500));

        long totalStudents = userRepo.countByRole(Role.STUDENT);
        long activeAssessments = assessmentRepo.countActiveAt(LocalDateTime.now());
        double avgScore = Optional.ofNullable(attemptRepo.averageSubmittedScore()).orElse(0.0);
        long studentsWithZeroAttempts = userRepo.countByRoleWithNoAttempts(Role.STUDENT);

        Map<String, Long> branchBreakdown = userRepo.branchBreakdownByRole(Role.STUDENT).stream()
                .collect(Collectors.toMap(
                        row -> row[0] == null ? "UNKNOWN" : String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        Map<Integer, Long> batchBreakdown = userRepo.batchBreakdownByRole(Role.STUDENT).stream()
                .collect(Collectors.toMap(
                        row -> row[0] == null ? 0 : ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new
                ));

        List<Attempt> recentAttempts = attemptRepo.findRecentAttempts(
                PageRequest.of(0, safeRecentLimit)
        ).getContent();
        List<Map<String, Object>> recentActivity = recentAttempts.stream()
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

        List<LeaderboardScore> top = leaderboardScoreRepo.findAllByOrderByTotalScoreDescLastUpdatedAsc(
                PageRequest.of(0, 5)
        ).getContent();
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
        List<Map<String, Object>> lowPerformers = userRepo.findLowPerformers(
                        Role.STUDENT,
                        twoWeeksAgo,
                        PageRequest.of(0, safeLowPerformerLimit, Sort.by("name").ascending())
                ).getContent().stream()
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
        response.put("totalStudents", totalStudents);
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
