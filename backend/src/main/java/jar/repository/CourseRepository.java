package jar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jar.entity.Course;

public interface CourseRepository
        extends JpaRepository<Course, Long> {
}