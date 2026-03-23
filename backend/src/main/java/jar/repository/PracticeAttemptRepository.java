package jar.repository;

import jar.entity.PracticeAttempt;
import jar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PracticeAttemptRepository extends JpaRepository<PracticeAttempt, Long> {
    List<PracticeAttempt> findByStudent(User student);

    long countByStudentAndCorrectTrueAndAttemptedAtBetween(User student, LocalDateTime start, LocalDateTime end);
}
