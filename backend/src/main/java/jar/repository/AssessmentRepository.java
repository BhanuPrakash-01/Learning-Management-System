package jar.repository;

import jar.entity.Assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AssessmentRepository
        extends JpaRepository<Assessment, Long> {

    @Query("SELECT a FROM Assessment a WHERE a.active IS NULL OR a.active = TRUE")
    List<Assessment> findActiveOrUnspecified();

    Optional<Assessment> findFirstByTitleIgnoreCase(String title);

    @Query("""
            SELECT COUNT(a) FROM Assessment a
            WHERE a.startTime IS NOT NULL
              AND a.endTime IS NOT NULL
              AND :now >= a.startTime
              AND :now <= a.endTime
            """)
    long countActiveAt(@Param("now") LocalDateTime now);
}
