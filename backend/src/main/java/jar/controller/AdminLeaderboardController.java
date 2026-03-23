package jar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/leaderboard")
public class AdminLeaderboardController {

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of(
                "assessmentAttemptPoints", 10,
                "assessmentScoreFactor", 0.9,
                "practiceCorrectPoints", 1,
                "practiceDailyCap", 50,
                "codingEasy", 10,
                "codingMedium", 25,
                "codingHard", 50,
                "streakBonusEach7Days", 20
        );
    }
}
