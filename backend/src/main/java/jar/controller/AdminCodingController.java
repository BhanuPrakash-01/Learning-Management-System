package jar.controller;

import jar.dto.CodingProblemRequest;
import jar.entity.CodingProblem;
import jar.repository.CodingProblemRepository;
import jar.service.security.AdminAuditService;
import jar.service.security.InputSanitizerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coding")
public class AdminCodingController {

    private final CodingProblemRepository problemRepo;
    private final InputSanitizerService sanitizer;
    private final AdminAuditService adminAuditService;

    public AdminCodingController(CodingProblemRepository problemRepo,
                                 InputSanitizerService sanitizer,
                                 AdminAuditService adminAuditService) {
        this.problemRepo = problemRepo;
        this.sanitizer = sanitizer;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/problems")
    public List<CodingProblem> getProblems() {
        return problemRepo.findAll();
    }

    @PostMapping("/problems")
    public CodingProblem createProblem(@RequestBody CodingProblemRequest request, Authentication auth) {
        String difficulty = request.getDifficulty() == null
                ? "EASY"
                : sanitizer.sanitizePlainText(request.getDifficulty());
        if (difficulty == null || difficulty.isBlank()) {
            difficulty = "EASY";
        }

        CodingProblem problem = CodingProblem.builder()
                .title(sanitizer.sanitizePlainText(request.getTitle()))
                .description(sanitizer.sanitizeRichText(request.getDescription()))
                .difficulty(difficulty.toUpperCase())
                .topic(sanitizer.sanitizePlainText(request.getTopic()))
                .sampleInput(sanitizer.sanitizePlainText(request.getSampleInput()))
                .sampleOutput(sanitizer.sanitizePlainText(request.getSampleOutput()))
                .constraintsText(sanitizer.sanitizeRichText(request.getConstraintsText()))
                .hints(sanitizer.sanitizeRichText(request.getHints()))
                .testCases(request.getTestCases())
                .build();
        CodingProblem saved = problemRepo.save(problem);
        adminAuditService.log(auth.getName(), "CREATE_CODING_PROBLEM", "CODING_PROBLEM", String.valueOf(saved.getId()),
                "Created coding problem");
        return saved;
    }
}
