package jar.controller;

import jar.dto.AnswerRequest;
import jar.entity.Attempt;
import jar.service.AttemptService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/attempts")
public class AttemptController {

    private final AttemptService attemptService;

    public AttemptController(AttemptService attemptService) {
        this.attemptService = attemptService;
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
}