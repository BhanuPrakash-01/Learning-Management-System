package jar.service.impl;

import jar.dto.AssessmentRequest;
import jar.entity.Assessment;
import jar.entity.Course;
import jar.repository.AssessmentRepository;
import jar.repository.CourseRepository;
import jar.service.AssessmentService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepo;
    private final CourseRepository courseRepo;

    public AssessmentServiceImpl(AssessmentRepository assessmentRepo,
                                 CourseRepository courseRepo) {
        this.assessmentRepo = assessmentRepo;
        this.courseRepo = courseRepo;
    }

    @Override
    public Assessment createAssessment(AssessmentRequest request) {

        Course course = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Assessment assessment = Assessment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .duration(request.getDuration())
                .course(course)
                .build();

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
    public List<Assessment> getByCourse(Long courseId) {

        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        return assessmentRepo.findByCourse(course);
    }

    @Override
    public Assessment updateAssessment(Long id, AssessmentRequest request) {

        Assessment assessment = assessmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        Course course = courseRepo.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        assessment.setTitle(request.getTitle());
        assessment.setDescription(request.getDescription());
        assessment.setDuration(request.getDuration());
        assessment.setCourse(course);

        return assessmentRepo.save(assessment);
    }

    @Override
    public void deleteAssessment(Long id) {
        assessmentRepo.deleteById(id);
    }
}