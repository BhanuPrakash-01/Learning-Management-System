package jar.repository;

import jar.entity.LeaderboardScore;
import jar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardScoreRepository extends JpaRepository<LeaderboardScore, Long> {
    Optional<LeaderboardScore> findByStudent(User student);

    List<LeaderboardScore> findAllByOrderByTotalScoreDescLastUpdatedAsc();
}
