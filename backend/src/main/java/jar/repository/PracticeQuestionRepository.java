package jar.repository;

import jar.entity.PracticeQuestion;
import jar.entity.PracticeTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {
    List<PracticeQuestion> findByTopic(PracticeTopic topic);

    long countByTopic(PracticeTopic topic);
}
