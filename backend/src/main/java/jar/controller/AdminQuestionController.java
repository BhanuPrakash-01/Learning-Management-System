package jar.controller;

import jar.dto.QuestionRequest;
import jar.entity.Question;
import jar.service.QuestionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/library")
    public List<Question> getLibrary(@RequestParam(required = false) String topic,
                                     @RequestParam(required = false) String difficulty,
                                     @RequestParam(required = false) Integer randomCount) {
        List<Question> all = new ArrayList<>(questionService.getAll().stream()
                .filter(question -> topic == null || topic.isBlank() ||
                        (question.getTopic() != null && question.getTopic().equalsIgnoreCase(topic)))
                .filter(question -> difficulty == null || difficulty.isBlank() ||
                        (question.getDifficulty() != null && question.getDifficulty().equalsIgnoreCase(difficulty)))
                .toList());

        if (randomCount == null || randomCount <= 0 || randomCount >= all.size()) {
            return all;
        }
        Collections.shuffle(all);
        return all.subList(0, randomCount);
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

    // ✅ NEW: Bulk CSV upload endpoint
    //
    // Uses @RequestParam with MultipartFile — Spring automatically handles
    // multipart/form-data content type when this annotation is present.
    //
    // Why a separate endpoint vs. modifying the existing POST?
    //   - Keeps concerns separate: JSON body vs. file upload are different contracts
    //   - Easier to add auth/rate-limiting to bulk ops independently
    //   - Mirrors how real platforms expose bulk APIs (Canvas API has /quiz_questions/bulk)
    //
    // Returns the count of questions saved, not the full list, to keep the
    // response lean — the admin can reload the questions list separately.
    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUpload(@RequestParam("file") MultipartFile file) {
        try {
            List<Question> saved = questionService.bulkUpload(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully uploaded " + saved.size() + " questions",
                    "count", saved.size()
            ));
        } catch (Exception e) {
            // Return the full error message so admins can see which rows failed
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
