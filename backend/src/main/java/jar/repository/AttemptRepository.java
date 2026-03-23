package jar.repository;

import jar.entity.Attempt;
import jar.entity.User;
import jar.entity.Assessment;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
