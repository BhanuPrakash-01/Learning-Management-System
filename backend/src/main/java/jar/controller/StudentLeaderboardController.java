package jar.controller;

import jar.entity.LeaderboardScore;
import jar.entity.User;
import jar.repository.UserRepository;
import jar.service.impl.LeaderboardServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/student/leaderboard")
public class StudentLeaderboardController {

    private final UserRepository userRepo;
    private final LeaderboardServiceImpl leaderboardService;

    public StudentLeaderboardController(UserRepository userRepo,
                                        LeaderboardServiceImpl leaderboardService) {
        this.userRepo = userRepo;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public List<Map<String, Object>> leaderboard(Authentication auth,
                                                 @RequestParam(required = false, defaultValue = "global") String scope,
                                                 @RequestParam(required = false) Long assessmentId) {
        User current = userRepo.findByEmail(auth.getName()).orElseThrow(() -> new RuntimeException("Student not found"));
        List<LeaderboardScore> scores = leaderboardService.getScores(scope, current, assessmentId);
        AtomicInteger rank = new AtomicInteger(1);
        return scores.stream().map(score -> {
            Map<String, Object> row = new HashMap<>();
            row.put("rank", rank.getAndIncrement());
            row.put("studentId", score.getStudent().getId());
            row.put("name", score.getStudent().getName());
            row.put("rollNumber", score.getStudent().getRollNumber());
            row.put("branch", score.getStudent().getBranch());
            row.put("batchYear", score.getStudent().getBatchYear());
            row.put("section", score.getStudent().getSection());
            row.put("totalScore", score.getTotalScore());
            row.put("assessmentScore", score.getAssessmentScore());
            row.put("practiceScore", score.getPracticeScore());
            row.put("codingScore", score.getCodingScore());
            row.put("lastUpdated", score.getLastUpdated());
            row.put("isCurrentUser", score.getStudent().getId().equals(current.getId()));
            return row;
        }).toList();
    }
}
