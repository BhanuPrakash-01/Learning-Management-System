package jar.repository;

import jar.entity.PracticeStreak;
import jar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeStreakRepository extends JpaRepository<PracticeStreak, Long> {
    Optional<PracticeStreak> findByStudent(User student);
}
