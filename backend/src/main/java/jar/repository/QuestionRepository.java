package jar.repository;

import jar.entity.Question;
import jar.entity.Assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository
        extends JpaRepository<Question, Long> {

    List<Question> findByAssessment(Assessment assessment);

    long countByAssessment(Assessment assessment);
}
