package jar.controller;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.service.AssessmentService;
import jar.service.security.AdminAuditService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/assessments")
public class AdminAssessmentController {

    private final AssessmentService assessmentService;
    private final AdminAuditService adminAuditService;

    public AdminAssessmentController(AssessmentService assessmentService,
                                     AdminAuditService adminAuditService) {
        this.assessmentService = assessmentService;
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
