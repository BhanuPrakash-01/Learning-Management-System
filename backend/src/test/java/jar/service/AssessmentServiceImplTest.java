package jar.service;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.CourseRepository;
import jar.repository.EnrollmentRepository;
import jar.repository.UserRepository;
import jar.service.impl.AssessmentServiceImpl;
import jar.service.security.InputSanitizerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private InputSanitizerService sanitizer;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    @Test
    void shouldFilterAssessmentByStudentTargeting() {
        User user = User.builder()
                .email("student@anurag.ac.in")
                .branch("CSE")
                .batchYear(2025)
                .section("A")
                .build();

        Assessment visible = Assessment.builder()
                .id(1L)
                .title("Visible")
                .targetBranches("CSE")
                .targetBatches("2025")
                .targetSections("A")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        Assessment hidden = Assessment.builder()
                .id(2L)
                .title("Hidden")
                .targetBranches("ECE")
                .targetBatches("2024")
                .targetSections("B")
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        when(userRepository.findByEmail("student@anurag.ac.in")).thenReturn(Optional.of(user));
        when(assessmentRepository.findActiveOrUnspecified()).thenReturn(List.of(visible, hidden));

        List<Assessment> assessments = assessmentService.getVisibleForStudent("student@anurag.ac.in");

        assertEquals(1, assessments.size());
        assertEquals("Visible", assessments.get(0).getTitle());
    }

    @Test
    void shouldCreateAssessmentWithTargetArrays() {
        AssessmentRequest request = new AssessmentRequest();
        request.setTitle("Test");
        request.setDuration(30);
        request.setTargetBranches(List.of("CSE", "IT"));
        request.setTargetBatchYears(List.of(2024, 2025));
        request.setTargetSections(List.of("A"));

        when(assessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sanitizer.sanitizePlainText(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sanitizer.sanitizeRichText(any())).thenAnswer(inv -> inv.getArgument(0));

        Assessment saved = assessmentService.createAssessment(request);

        assertEquals("CSE,IT", saved.getTargetBranches());
        assertEquals("2024,2025", saved.getTargetBatches());
        assertEquals("A", saved.getTargetSections());
    }
}
