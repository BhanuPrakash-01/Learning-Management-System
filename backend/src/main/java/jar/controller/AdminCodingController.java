package jar.controller;

import jar.dto.CodingProblemRequest;
import jar.entity.CodingProblem;
import jar.repository.CodingProblemRepository;
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

    public AdminCodingController(CodingProblemRepository problemRepo) {
        this.problemRepo = problemRepo;
    }

    @GetMapping("/problems")
    public List<CodingProblem> getProblems() {
        return problemRepo.findAll();
    }

    @PostMapping("/problems")
    public CodingProblem createProblem(@RequestBody CodingProblemRequest request) {
        CodingProblem problem = CodingProblem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .difficulty(request.getDifficulty() == null ? "EASY" : request.getDifficulty().toUpperCase())
                .topic(request.getTopic())
                .sampleInput(request.getSampleInput())
                .sampleOutput(request.getSampleOutput())
                .constraintsText(request.getConstraintsText())
                .hints(request.getHints())
                .testCases(request.getTestCases())
                .build();
        return problemRepo.save(problem);
    }
}
