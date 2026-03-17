package jar.controller;

import jar.dto.QuestionRequest;
import jar.entity.Question;
import jar.service.QuestionService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public List<Question> getAll() {
        return questionService.getAll();
    }

    @GetMapping("/assessment/{assessmentId}")
    public List<Question> getByAssessment(@PathVariable Long assessmentId) {
        return questionService.getByAssessment(assessmentId);
    }

    @PostMapping
    public Question add(@RequestBody QuestionRequest request) {
        return questionService.addQuestion(request);
    }

    @PutMapping("/{id}")
    public Question update(@PathVariable Long id,
                           @RequestBody QuestionRequest request) {
        return questionService.updateQuestion(id, request);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return "Question deleted successfully";
    }
}