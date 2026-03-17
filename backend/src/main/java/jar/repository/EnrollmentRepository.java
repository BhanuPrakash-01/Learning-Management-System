package jar.repository;

import jar.entity.Enrollment;
import jar.entity.User;
import jar.entity.Course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository
        extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentAndCourse(User student, Course course);

    List<Enrollment> findByStudent(User student);
}