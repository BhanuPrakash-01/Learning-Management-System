package jar.controller;

import jar.entity.Question;
import jar.service.QuestionService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/questions")
public class StudentQuestionController {

    private final QuestionService questionService;

    public StudentQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping("/{assessmentId}")
    public List<Question> get(@PathVariable Long assessmentId) {
        return questionService.getByAssessment(assessmentId);
    }
}