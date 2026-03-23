package jar.controller;

import jar.entity.Assessment;
import jar.service.AssessmentService;

import org.springframework.security.core.Authentication;
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
    public List<Assessment> getVisibleForMe(Authentication auth) {
        return assessmentService.getVisibleForStudent(auth.getName());
    }
}
