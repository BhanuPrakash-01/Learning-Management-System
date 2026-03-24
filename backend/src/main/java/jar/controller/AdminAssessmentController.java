package jar.controller;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.entity.Question;
import jar.service.AssessmentService;
import jar.service.QuestionService;
import jar.service.security.AdminAuditService;

import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/assessments")
public class AdminAssessmentController {

    private final AssessmentService assessmentService;
    private final QuestionService questionService;
    private final AdminAuditService adminAuditService;

    public AdminAssessmentController(AssessmentService assessmentService,
                                     QuestionService questionService,
                                     AdminAuditService adminAuditService) {
        this.assessmentService = assessmentService;
        this.questionService = questionService;
        this.adminAuditService = adminAuditService;
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
    public Assessment create(@RequestBody AssessmentRequest request, Authentication auth) {
        Assessment saved = assessmentService.createAssessment(request);
        adminAuditService.log(auth.getName(), "CREATE_ASSESSMENT", "ASSESSMENT",
                String.valueOf(saved.getId()), "Assessment created");
        return saved;
    }

    @PostMapping(value = "/with-questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> createWithQuestions(@RequestPart("assessmentJson") AssessmentRequest request,
                                                 @RequestPart(value = "file", required = false) MultipartFile file,
                                                 Authentication auth) {
        Assessment savedAssessment = assessmentService.createAssessment(request);
        List<Question> createdQuestions = List.of();
        if (file != null && !file.isEmpty()) {
            createdQuestions = questionService.bulkUpload(file, savedAssessment);
        }

        adminAuditService.log(auth.getName(), "CREATE_ASSESSMENT_WITH_QUESTIONS", "ASSESSMENT",
                String.valueOf(savedAssessment.getId()),
                "Created assessment " + savedAssessment.getTitle() + " with " + createdQuestions.size() + " questions");

        return ResponseEntity.ok(Map.of(
                "assessment", savedAssessment,
                "questionCount", createdQuestions.size()
        ));
    }

    @PutMapping("/{id}")
    public Assessment update(@PathVariable Long id,
                             @RequestBody AssessmentRequest request,
                             Authentication auth) {
        Assessment updated = assessmentService.updateAssessment(id, request);
        adminAuditService.log(auth.getName(), "UPDATE_ASSESSMENT", "ASSESSMENT",
                String.valueOf(updated.getId()), "Assessment updated");
        return updated;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, Authentication auth) {
        assessmentService.deleteAssessment(id);
        adminAuditService.log(auth.getName(), "DELETE_ASSESSMENT", "ASSESSMENT",
                String.valueOf(id), "Assessment deleted");
        return "Assessment deleted successfully";
    }
}
