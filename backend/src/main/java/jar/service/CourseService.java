package jar.service;

import java.util.List;
import jar.dto.CourseRequest;
import jar.entity.Course;

public interface CourseService {

    Course createCourse(CourseRequest request);

    List<Course> getAllCourses();

    Course getCourseById(Long id);

    Course updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);
}