package jar.controller;

import jar.dto.QuestionRequest;
import jar.entity.Question;
import jar.service.QuestionService;
import jar.service.security.AdminAuditService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;
    private final AdminAuditService adminAuditService;

    public AdminQuestionController(QuestionService questionService,
                                   AdminAuditService adminAuditService) {
        this.questionService = questionService;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public Map<String, Object> getAll(@RequestParam(required = false, defaultValue = "0") int page,
                                      @RequestParam(required = false, defaultValue = "25") int size) {
        Page<Question> result = questionService.getAll(
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("id").descending())
        );
        return Map.of(
                "content", result.getContent(),
                "page", page,
                "size", size,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @GetMapping("/assessment/{assessmentId}")
    public List<Question> getByAssessment(@PathVariable Long assessmentId) {
        return questionService.getByAssessment(assessmentId);
    }

    @GetMapping("/library")
    public Map<String, Object> getLibrary(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) Long assessmentId,
                                          @RequestParam(required = false) String topic,
                                          @RequestParam(required = false) String difficulty,
                                          @RequestParam(required = false, defaultValue = "0") int page,
                                          @RequestParam(required = false, defaultValue = "25") int size) {
        Page<Question> result = questionService.searchLibrary(
                search,
                assessmentId,
                difficulty,
                topic,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("id").descending())
        );

        return Map.of(
                "content", result.getContent(),
                "page", page,
                "size", size,
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "assessmentCounts", questionService.assessmentQuestionCounts()
        );
    }

    @PostMapping
    public Question add(@RequestBody QuestionRequest request, Authentication auth) {
        Question saved = questionService.addQuestion(request);
        adminAuditService.log(auth.getName(), "CREATE_QUESTION", "QUESTION", String.valueOf(saved.getId()),
                "Question created");
        return saved;
    }

    @PutMapping("/{id}")
    public Question update(@PathVariable Long id,
                           @RequestBody QuestionRequest request,
                           Authentication auth) {
        Question updated = questionService.updateQuestion(id, request);
        adminAuditService.log(auth.getName(), "UPDATE_QUESTION", "QUESTION", String.valueOf(updated.getId()),
                "Question updated");
        return updated;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Authentication auth) {
        questionService.deleteQuestion(id);
        adminAuditService.log(auth.getName(), "DELETE_QUESTION", "QUESTION", String.valueOf(id),
                "Question deleted");
        return "Question deleted successfully";
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<?> bulkDelete(@RequestBody Map<String, List<Long>> body,
                                        Authentication auth) {
        List<Long> ids = body.getOrDefault("ids", List.of());
        questionService.deleteQuestions(ids);
        adminAuditService.log(auth.getName(), "BULK_DELETE_QUESTION", "QUESTION", String.valueOf(ids.size()),
                "Deleted " + ids.size() + " questions");
        return ResponseEntity.ok(Map.of(
                "message", "Deleted " + ids.size() + " questions",
                "count", ids.size()
        ));
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
    public ResponseEntity<?> bulkUpload(@RequestParam("file") MultipartFile file,
                                        Authentication auth) {
        try {
            List<Question> saved = questionService.bulkUpload(file);
            adminAuditService.log(auth.getName(), "BULK_UPLOAD_QUESTION", "QUESTION", String.valueOf(saved.size()),
                    "Uploaded " + saved.size() + " questions");
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
