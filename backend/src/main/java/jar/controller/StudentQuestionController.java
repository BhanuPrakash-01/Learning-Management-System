package jar.controller;

import jar.entity.Question;
import jar.service.AssessmentService;
import jar.service.QuestionService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/questions")
public class StudentQuestionController {

    private final QuestionService questionService;
    private final AssessmentService assessmentService;

    public StudentQuestionController(QuestionService questionService,
                                     AssessmentService assessmentService) {
        this.questionService = questionService;
        this.assessmentService = assessmentService;
    }

    @GetMapping("/{assessmentId}")
    public List<Question> get(@PathVariable Long assessmentId, Authentication auth) {
        boolean hasAccess = assessmentService.getVisibleForStudent(auth.getName()).stream()
                .anyMatch(assessment -> assessment.getId().equals(assessmentId));
        if (!hasAccess) {
            throw new RuntimeException("Assessment not available for this student");
        }
        return questionService.getByAssessment(assessmentId);
    }
}
