package jar.service.impl;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.entity.Course;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.CourseRepository;
import jar.repository.EnrollmentRepository;
import jar.repository.UserRepository;
import jar.service.AssessmentService;
import jar.service.security.InputSanitizerService;
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
    private final CourseRepository courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final InputSanitizerService sanitizer;

    public AssessmentServiceImpl(AssessmentRepository assessmentRepo,
                                 UserRepository userRepo,
                                 CourseRepository courseRepo,
                                 EnrollmentRepository enrollmentRepo,
                                 InputSanitizerService sanitizer) {
        this.assessmentRepo = assessmentRepo;
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.sanitizer = sanitizer;
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
                .filter(a -> a.getCourse() == null || enrollmentRepo.existsByStudentAndCourse(student, a.getCourse()))
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

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepo.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));
        }

        assessment.setTitle(sanitizer.sanitizePlainText(request.getTitle().trim()));
        assessment.setDescription(sanitizer.sanitizeRichText(request.getDescription()));
        assessment.setDuration(request.getDuration());
        assessment.setAssessmentType(request.getAssessmentType() == null || request.getAssessmentType().isBlank()
                ? "WEEKLY_TEST" : sanitizer.sanitizePlainText(request.getAssessmentType()));
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
        assessment.setCourse(course);
        assessment.setReviewAfterClose(Boolean.TRUE.equals(request.getReviewAfterClose()));
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
