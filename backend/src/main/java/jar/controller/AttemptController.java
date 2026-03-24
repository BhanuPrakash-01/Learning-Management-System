package jar.controller;

import jar.dto.AnswerRequest;
import jar.entity.Attempt;
import jar.entity.AttemptAnswer;
import jar.repository.AttemptAnswerRepository;
import jar.repository.AttemptRepository;
import jar.service.AttemptService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/student/attempts")
public class AttemptController {

    private final AttemptService attemptService;
    private final AttemptRepository attemptRepo;
    private final AttemptAnswerRepository answerRepo;

    public AttemptController(AttemptService attemptService,
                             AttemptRepository attemptRepo,
                             AttemptAnswerRepository answerRepo) {
        this.attemptService = attemptService;
        this.attemptRepo = attemptRepo;
        this.answerRepo = answerRepo;
    }

    @PostMapping("/start/{assessmentId}")
    public Attempt start(@PathVariable Long assessmentId,
                          Authentication auth) {

        return attemptService.startAttempt(
                assessmentId,
                auth.getName()
        );
    }

    @PostMapping("/{attemptId}/answer")
    public String saveAnswer(@PathVariable Long attemptId,
                             @RequestBody AnswerRequest request) {

        attemptService.saveAnswer(attemptId, request);
        return "Answer saved";
    }

    @PostMapping("/{attemptId}/submit")
    public Attempt submit(@PathVariable Long attemptId) {
        return attemptService.submitAttempt(attemptId);
    }

    @GetMapping("/my")
    public List<Attempt> myAttempts(Authentication auth) {
        return attemptService.getMyAttempts(auth.getName());
    }

    @GetMapping("/{attemptId}/review")
    public Map<String, Object> review(@PathVariable Long attemptId, Authentication auth) {
        Attempt attempt = attemptRepo.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (attempt.getStudent() == null || !attempt.getStudent().getEmail().equals(auth.getName())) {
            throw new RuntimeException("Unauthorized");
        }
        if (attempt.getAssessment() != null
                && Boolean.TRUE.equals(attempt.getAssessment().getReviewAfterClose())
                && attempt.getAssessment().getEndTime() != null) {
            LocalDateTime closeAt = attempt.getAssessment().getEndTime();
            if (Boolean.TRUE.equals(attempt.getAssessment().getAllowLateSubmission())) {
                closeAt = closeAt.plusMinutes(15);
            }
            if (LocalDateTime.now().isBefore(closeAt)) {
                throw new RuntimeException("Review will be available only after the assessment window closes.");
            }
        }

        List<AttemptAnswer> answers = answerRepo.findByAttempt(attempt);
        Map<String, Object> response = new HashMap<>();
        response.put("attempt", attempt);
        response.put("answers", answers.stream().map(answer -> {
            Map<String, Object> row = new HashMap<>();
            row.put("questionId", answer.getQuestion().getId());
            row.put("questionText", answer.getQuestion().getQuestionText());
            row.put("yourAnswer", answer.getSelectedAnswer());
            row.put("correctAnswer", answer.getQuestion().getCorrectAnswer());
            row.put("explanation", "");
            row.put("correct", answer.getSelectedAnswer() != null
                    && answer.getSelectedAnswer().equalsIgnoreCase(answer.getQuestion().getCorrectAnswer()));
            return row;
        }).toList());
        return response;
    }
}
