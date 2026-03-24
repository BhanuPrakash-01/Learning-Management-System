package jar.repository;

import jar.entity.Attempt;
import jar.entity.User;
import jar.entity.Assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttemptRepository
        extends JpaRepository<Attempt, Long> {

    Optional<Attempt> findByStudentAndAssessmentAndSubmittedFalse(
            User student,
            Assessment assessment
    );

    List<Attempt> findByStudent(User student);

    List<Attempt> findByAssessment(Assessment assessment);

    long countByStudentAndAssessmentAndSubmittedTrue(User student, Assessment assessment);

    List<Attempt> findByAssessmentAndSubmittedTrue(Assessment assessment);

    List<Attempt> findBySubmittedFalseAndDeadlineBefore(LocalDateTime now);

    @Query("""
            SELECT COUNT(a) > 0 FROM Attempt a
            WHERE a.student = :student
              AND a.startTime IS NOT NULL
              AND a.startTime > :after
            """)
    boolean existsRecentAttemptForStudent(@Param("student") User student,
                                          @Param("after") LocalDateTime after);

    @Query("SELECT COALESCE(AVG(a.score), 0) FROM Attempt a WHERE a.submitted = TRUE")
    Double averageSubmittedScore();

    @Query("SELECT a FROM Attempt a ORDER BY COALESCE(a.endTime, a.startTime) DESC")
    Page<Attempt> findRecentAttempts(Pageable pageable);
}
