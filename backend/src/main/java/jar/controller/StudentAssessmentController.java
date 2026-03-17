package jar.controller;

import jar.entity.Assessment;
import jar.service.AssessmentService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/assessments")
public class StudentAssessmentController {

    private final AssessmentService assessmentService;

    public StudentAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<Assessment> getAll() {
        return assessmentService.getAll();
    }

    @GetMapping("/course/{courseId}")
    public List<Assessment> byCourse(@PathVariable Long courseId) {
        return assessmentService.getByCourse(courseId);
    }
}