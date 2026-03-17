package jar.controller;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.service.AssessmentService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assessments")
public class AdminAssessmentController {

    private final AssessmentService assessmentService;

    public AdminAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<Assessment> getAll() {
        return assessmentService.getAll();
    }

    @GetMapping("/{id}")
    public Assessment getById(@PathVariable Long id) {
        return assessmentService.getById(id);
    }

    @PostMapping
    public Assessment create(@RequestBody AssessmentRequest request) {
        return assessmentService.createAssessment(request);
    }

    @PutMapping("/{id}")
    public Assessment update(@PathVariable Long id,
                             @RequestBody AssessmentRequest request) {
        return assessmentService.updateAssessment(id, request);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return "Assessment deleted successfully";
    }
}