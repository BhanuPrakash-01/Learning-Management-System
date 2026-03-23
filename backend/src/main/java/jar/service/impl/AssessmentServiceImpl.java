package jar.service.impl;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.UserRepository;
import jar.service.AssessmentService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final UserRepository userRepo;

    public AssessmentServiceImpl(AssessmentRepository assessmentRepo, UserRepository userRepo) {
        this.assessmentRepo = assessmentRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Assessment createAssessment(AssessmentRequest request) {
        Assessment assessment = new Assessment();
        applyRequest(assessment, request);
        return assessmentRepo.save(assessment);
    }

    @Override
    public List<Assessment> getAll() {
        return assessmentRepo.findAll();
    }

    @Override
    public Assessment getById(Long id) {
        return assessmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
    }

    @Override
    public List<Assessment> getVisibleForStudent(String email) {
        User student = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        LocalDateTime now = LocalDateTime.now();

        return assessmentRepo.findActiveOrUnspecified()
                .stream()
                .filter(a -> matchesTargets(a.getTargetBranches(), student.getBranch()))
                .filter(a -> matchesTargets(a.getTargetBatches(),
                        student.getBatchYear() == null ? null : String.valueOf(student.getBatchYear())))
                .filter(a -> matchesTargets(a.getTargetSections(), student.getSection()))
                .filter(a -> a.getStartTime() == null || !now.isBefore(a.getStartTime()))
                .filter(a -> a.getEndTime() == null || !now.isAfter(a.getEndTime()))
                .toList();
    }

    @Override
    public Assessment updateAssessment(Long id, AssessmentRequest request) {
        Assessment assessment = assessmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        applyRequest(assessment, request);
        return assessmentRepo.save(assessment);
    }

    @Override
    public void deleteAssessment(Long id) {
        assessmentRepo.deleteById(id);
    }

    private void applyRequest(Assessment assessment, AssessmentRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("Assessment title is required");
        }
        if (request.getDuration() <= 0) {
            throw new RuntimeException("Duration must be greater than 0");
        }
        if (request.getStartTime() != null && request.getEndTime() != null
                && request.getEndTime().isBefore(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        assessment.setTitle(request.getTitle().trim());
        assessment.setDescription(request.getDescription());
        assessment.setDuration(request.getDuration());
        assessment.setAssessmentType(request.getAssessmentType() == null || request.getAssessmentType().isBlank()
                ? "WEEKLY_TEST" : request.getAssessmentType());
        assessment.setTargetBranches(joinList(request.getTargetBranches()));
        assessment.setTargetBatches(joinList(request.getTargetBatchYears() == null ? null :
                request.getTargetBatchYears().stream().map(String::valueOf).toList()));
        assessment.setTargetSections(joinList(request.getTargetSections()));
        assessment.setStartTime(request.getStartTime());
        assessment.setEndTime(request.getEndTime());
        assessment.setAllowLateSubmission(Boolean.TRUE.equals(request.getAllowLateSubmission()));
        assessment.setMaxAttempts(request.getMaxAttempts() == null || request.getMaxAttempts() < 1
                ? 1 : request.getMaxAttempts());
        assessment.setNegativeMarking(Boolean.TRUE.equals(request.getNegativeMarking()));
        assessment.setPenaltyFraction(request.getPenaltyFraction() == null
                ? new BigDecimal("0.25") : request.getPenaltyFraction());
        if (assessment.getActive() == null) {
            assessment.setActive(true);
        }
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.joining(","));
    }

    private boolean matchesTargets(String targets, String value) {
        if (targets == null || targets.isBlank()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        Set<String> values = Arrays.stream(targets.split(","))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .collect(Collectors.toSet());
        return values.contains(value);
    }
}
