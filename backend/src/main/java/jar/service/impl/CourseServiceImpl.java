package jar.service.impl;

import jar.dto.CourseRequest;
import jar.entity.Course;
import jar.repository.CourseRepository;
import jar.service.CourseService;
import jar.service.security.InputSanitizerService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final InputSanitizerService sanitizer;

    public CourseServiceImpl(CourseRepository courseRepository,
                             InputSanitizerService sanitizer) {
        this.courseRepository = courseRepository;
        this.sanitizer = sanitizer;
    }

    @Override
    public Course createCourse(CourseRequest request) {

        Course course = Course.builder()
                .title(sanitizer.sanitizePlainText(request.getTitle()))
                .description(sanitizer.sanitizeRichText(request.getDescription()))
                .instructor(sanitizer.sanitizePlainText(request.getInstructor()))
                .duration(request.getDuration())
                .build();

        return courseRepository.save(course);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    @Override
    public Course updateCourse(Long id, CourseRequest request) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setTitle(sanitizer.sanitizePlainText(request.getTitle()));
        course.setDescription(sanitizer.sanitizeRichText(request.getDescription()));
        course.setInstructor(sanitizer.sanitizePlainText(request.getInstructor()));
        course.setDuration(request.getDuration());

        return courseRepository.save(course);
    }

    @Override
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}
