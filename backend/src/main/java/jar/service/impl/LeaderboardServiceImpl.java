package jar.service.impl;

import jar.entity.Attempt;
import jar.entity.CodingSubmission;
import jar.entity.LeaderboardScore;
import jar.entity.PracticeAttempt;
import jar.entity.Role;
import jar.entity.User;
import jar.repository.AttemptRepository;
import jar.repository.CodingSubmissionRepository;
import jar.repository.LeaderboardScoreRepository;
import jar.repository.PracticeAttemptRepository;
import jar.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardServiceImpl {

    private final UserRepository userRepo;
    private final AttemptRepository attemptRepo;
    private final PracticeAttemptRepository practiceAttemptRepo;
    private final CodingSubmissionRepository codingSubmissionRepo;
    private final LeaderboardScoreRepository leaderboardScoreRepo;

    public LeaderboardServiceImpl(UserRepository userRepo,
                                  AttemptRepository attemptRepo,
                                  PracticeAttemptRepository practiceAttemptRepo,
                                  CodingSubmissionRepository codingSubmissionRepo,
                                  LeaderboardScoreRepository leaderboardScoreRepo) {
        this.userRepo = userRepo;
        this.attemptRepo = attemptRepo;
        this.practiceAttemptRepo = practiceAttemptRepo;
        this.codingSubmissionRepo = codingSubmissionRepo;
        this.leaderboardScoreRepo = leaderboardScoreRepo;
    }

    @Scheduled(fixedRate = 900000)
    public void refreshLeaderboardScores() {
        List<User> users = userRepo.findByRole(Role.STUDENT);

        for (User user : users) {
            int assessmentScore = computeAssessmentScore(user);
            int practiceScore = computePracticeScore(user);
            int codingScore = computeCodingScore(user);
            int total = assessmentScore + practiceScore + codingScore;

            LeaderboardScore score = leaderboardScoreRepo.findByStudent(user)
                    .orElse(LeaderboardScore.builder().student(user).build());
            score.setAssessmentScore(assessmentScore);
            score.setPracticeScore(practiceScore);
            score.setCodingScore(codingScore);
            score.setTotalScore(total);
            score.setLastUpdated(LocalDateTime.now());
            leaderboardScoreRepo.save(score);
        }
    }

    public List<LeaderboardScore> getScores(String scope, User currentUser, Long assessmentId) {
        List<LeaderboardScore> all = new ArrayList<>(leaderboardScoreRepo.findAllByOrderByTotalScoreDescLastUpdatedAsc());
        if (scope == null || scope.isBlank() || scope.equals("global")) {
            return all;
        }
        return all.stream().filter(score -> {
            User student = score.getStudent();
            if (student == null) return false;
            return switch (scope) {
                case "branch" -> safeEquals(student.getBranch(), currentUser.getBranch());
                case "batch" -> student.getBatchYear() != null && student.getBatchYear().equals(currentUser.getBatchYear());
                case "section" -> safeEquals(student.getSection(), currentUser.getSection());
                case "assessment" -> assessmentId != null && hasAssessmentAttempt(student, assessmentId);
                default -> true;
            };
        }).toList();
    }

    private int computeAssessmentScore(User user) {
        List<Attempt> attempts = attemptRepo.findByStudent(user).stream()
                .filter(Attempt::isSubmitted)
                .toList();

        int points = 0;
        for (Attempt attempt : attempts) {
            points += 10; // any attempt
            int totalQ = Math.max(attempt.getTotalQuestions(), 1);
            double percentage = (attempt.getScore() / totalQ) * 100.0;
            points += (int) Math.round(percentage * 0.9);
            if (attempt.getAttemptNumber() == 1) {
                points += 5;
            }
        }
        return points;
    }

    private int computePracticeScore(User user) {
        List<PracticeAttempt> attempts = practiceAttemptRepo.findByStudent(user);
        int points = 0;
        List<LocalDate> days = attempts.stream()
                .filter(PracticeAttempt::isCorrect)
                .map(item -> item.getAttemptedAt().toLocalDate())
                .distinct()
                .toList();
        for (LocalDate day : days) {
            long dailyCorrect = attempts.stream()
                    .filter(a -> a.isCorrect() && a.getAttemptedAt().toLocalDate().equals(day))
                    .count();
            points += (int) Math.min(dailyCorrect, 50);
        }
        return Math.min(points, 5000);
    }

    private int computeCodingScore(User user) {
        List<CodingSubmission> submissions = codingSubmissionRepo.findByStudent(user);
        int points = 0;

        for (CodingSubmission submission : submissions) {
            if (!"ACCEPTED".equalsIgnoreCase(submission.getStatus())) {
                continue;
            }
            String difficulty = submission.getProblem() == null ? "" : submission.getProblem().getDifficulty();
            if ("EASY".equalsIgnoreCase(difficulty)) points += 10;
            else if ("MEDIUM".equalsIgnoreCase(difficulty)) points += 25;
            else if ("HARD".equalsIgnoreCase(difficulty)) points += 50;
        }

        return points;
    }

    private boolean hasAssessmentAttempt(User student, Long assessmentId) {
        return attemptRepo.findByStudent(student).stream()
                .anyMatch(attempt -> attempt.getAssessment() != null
                        && assessmentId.equals(attempt.getAssessment().getId())
                        && attempt.isSubmitted());
    }

    private boolean safeEquals(String left, String right) {
        if (left == null || right == null) return false;
        return left.equalsIgnoreCase(right);
    }
}
