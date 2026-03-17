package jar.repository;

import jar.entity.Attempt;
import jar.entity.AttemptAnswer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttemptAnswerRepository
        extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttempt(Attempt attempt);
}