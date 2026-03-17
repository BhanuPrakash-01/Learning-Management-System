package jar.repository;

import jar.entity.Assessment;
import jar.entity.Course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository
        extends JpaRepository<Assessment, Long> {

    List<Assessment> findByCourse(Course course);
}