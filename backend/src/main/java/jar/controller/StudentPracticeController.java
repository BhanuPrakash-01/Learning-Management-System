package jar.controller;

import jar.dto.PracticeAttemptRequest;
import jar.entity.PracticeAttempt;
import jar.entity.PracticeQuestion;
import jar.entity.PracticeStreak;
import jar.entity.PracticeTopic;
import jar.entity.User;
import jar.repository.PracticeAttemptRepository;
import jar.repository.PracticeCategoryRepository;
import jar.repository.PracticeQuestionRepository;
import jar.repository.PracticeStreakRepository;
import jar.repository.PracticeTopicRepository;
import jar.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/practice")
public class StudentPracticeController {

    private final PracticeCategoryRepository categoryRepo;
    private final PracticeTopicRepository topicRepo;
    private final PracticeQuestionRepository questionRepo;
    private final PracticeAttemptRepository attemptRepo;
    private final PracticeStreakRepository streakRepo;
    private final UserRepository userRepo;

    public StudentPracticeController(PracticeCategoryRepository categoryRepo,
                                     PracticeTopicRepository topicRepo,
                                     PracticeQuestionRepository questionRepo,
                                     PracticeAttemptRepository attemptRepo,
                                     PracticeStreakRepository streakRepo,
                                     UserRepository userRepo) {
        this.categoryRepo = categoryRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
        this.attemptRepo = attemptRepo;
        this.streakRepo = streakRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return categoryRepo.findAll().stream().map(category -> {
            List<PracticeTopic> topics = topicRepo.findByCategoryAndActiveTrue(category);
            Map<String, Object> row = new HashMap<>();
            row.put("id", category.getId());
            row.put("name", category.getName());
            row.put("topics", topics);
            return row;
        }).toList();
    }

    @GetMapping("/topics/{id}/questions")
    public List<PracticeQuestion> topicQuestions(@PathVariable Long id,
                                                 @RequestParam(required = false, defaultValue = "practice") String mode) {
        PracticeTopic topic = topicRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Topic not found"));
        if (!Boolean.TRUE.equals(topic.getActive())) {
            throw new NoSuchElementException("Topic not available");
        }
        List<PracticeQuestion> questions = questionRepo.findByTopic(topic);
        if ("test".equalsIgnoreCase(mode)) {
            return questions.stream().limit(10).toList();
        }
        return questions;
    }

    @PostMapping("/attempt")
    public Map<String, Object> attempt(Authentication auth,
                                       @RequestBody PracticeAttemptRequest request) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        PracticeQuestion question = questionRepo.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        boolean correct = request.getSelectedAnswer() != null
                && request.getSelectedAnswer().equalsIgnoreCase(question.getCorrectAnswer());

        PracticeAttempt attempt = PracticeAttempt.builder()
                .student(student)
                .question(question)
                .selectedAnswer(request.getSelectedAnswer())
                .correct(correct)
                .attemptedAt(LocalDateTime.now())
                .build();
        attemptRepo.save(attempt);

        PracticeStreak streak = streakRepo.findByStudent(student)
                .orElse(PracticeStreak.builder()
                        .student(student)
                        .currentStreak(0)
                        .bestStreak(0)
                        .build());
        LocalDate today = LocalDate.now();
        if (streak.getLastPracticeDate() == null) {
            streak.setCurrentStreak(1);
        } else if (streak.getLastPracticeDate().isEqual(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else if (!streak.getLastPracticeDate().isEqual(today)) {
            streak.setCurrentStreak(1);
        }
        streak.setLastPracticeDate(today);
        if (streak.getCurrentStreak() > streak.getBestStreak()) {
            streak.setBestStreak(streak.getCurrentStreak());
        }
        streakRepo.save(streak);

        List<PracticeAttempt> topicAttempts = attemptRepo.findByStudent(student).stream()
                .filter(item -> item.getQuestion() != null
                        && item.getQuestion().getTopic() != null
                        && item.getQuestion().getTopic().getId().equals(question.getTopic().getId()))
                .toList();
        long topicCorrect = topicAttempts.stream().filter(PracticeAttempt::isCorrect).count();
        double accuracy = topicAttempts.isEmpty() ? 0.0 : (topicCorrect * 100.0) / topicAttempts.size();

        Map<String, Object> response = new HashMap<>();
        response.put("correct", correct);
        response.put("explanation", question.getExplanation());
        response.put("currentStreak", streak.getCurrentStreak());
        response.put("bestStreak", streak.getBestStreak());
        response.put("topicAccuracy", Math.round(accuracy * 100.0) / 100.0);
        response.put("attemptedAt", attempt.getAttemptedAt());
        return response;
    }

    @GetMapping("/progress")
    public List<Map<String, Object>> topicProgress(Authentication auth) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        List<PracticeAttempt> attempts = attemptRepo.findByStudent(student);

        Map<Long, List<PracticeAttempt>> grouped = attempts.stream()
                .filter(item -> item.getQuestion() != null && item.getQuestion().getTopic() != null)
                .collect(Collectors.groupingBy(item -> item.getQuestion().getTopic().getId()));

        return grouped.entrySet().stream().map(entry -> {
            PracticeTopic topic = topicRepo.findById(entry.getKey()).orElse(null);
            long correct = entry.getValue().stream().filter(PracticeAttempt::isCorrect).count();
            double accuracy = entry.getValue().isEmpty() ? 0.0 : (correct * 100.0) / entry.getValue().size();

            Map<String, Object> row = new HashMap<>();
            row.put("topicId", entry.getKey());
            row.put("topic", topic == null ? "Unknown" : topic.getName());
            row.put("attempted", entry.getValue().size());
            row.put("accuracy", Math.round(accuracy * 100.0) / 100.0);
            row.put("completed", accuracy >= 80.0);
            return row;
        }).toList();
    }
}
