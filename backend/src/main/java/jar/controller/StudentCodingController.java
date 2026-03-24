package jar.controller;

import jar.dto.CodingSubmitRequest;
import jar.entity.CodingProblem;
import jar.entity.CodingSubmission;
import jar.entity.User;
import jar.repository.CodingProblemRepository;
import jar.repository.CodingSubmissionRepository;
import jar.repository.UserRepository;
import jar.service.impl.CodingExecutionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/coding")
public class StudentCodingController {

    private final CodingProblemRepository problemRepo;
    private final CodingSubmissionRepository submissionRepo;
    private final UserRepository userRepo;
    private final CodingExecutionService executionService;

    public StudentCodingController(CodingProblemRepository problemRepo,
                                   CodingSubmissionRepository submissionRepo,
                                   UserRepository userRepo,
                                   CodingExecutionService executionService) {
        this.problemRepo = problemRepo;
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
        this.executionService = executionService;
    }

    @GetMapping("/problems")
    public List<CodingProblem> getProblems(@RequestParam(required = false) String difficulty,
                                           @RequestParam(required = false) String topic) {
        if (difficulty != null && !difficulty.isBlank()) {
            return problemRepo.findByDifficulty(difficulty.toUpperCase());
        }
        if (topic != null && !topic.isBlank()) {
            return problemRepo.findByTopic(topic);
        }
        return problemRepo.findAll();
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(Authentication auth,
                                      @RequestBody CodingSubmitRequest request) {
        User student = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        CodingProblem problem = problemRepo.findById(request.getProblemId())
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Map<String, Object> eval = executionService.evaluate(request.getLanguage(), request.getCode(), problem.getTestCases());
        CodingSubmission submission = CodingSubmission.builder()
                .student(student)
                .problem(problem)
                .language(request.getLanguage())
                .code(request.getCode())
                .status(String.valueOf(eval.get("status")))
                .testCasesPassed((Integer) eval.getOrDefault("passed", 0))
                .submittedAt(LocalDateTime.now())
                .build();
        submissionRepo.save(submission);

        Map<String, Object> response = new HashMap<>();
        response.put("submissionId", submission.getId());
        response.put("status", submission.getStatus());
        response.put("testCasesPassed", submission.getTestCasesPassed());
        response.put("totalTestCases", eval.getOrDefault("total", 0));
        response.put("details", eval.getOrDefault("details", List.of()));
        response.put("submittedAt", submission.getSubmittedAt());
        response.put("source", eval.get("source"));
        return response;
    }
}
