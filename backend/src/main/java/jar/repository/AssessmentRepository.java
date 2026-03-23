package jar.repository;

import jar.entity.Assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository
        extends JpaRepository<Assessment, Long> {

    @Query("SELECT a FROM Assessment a WHERE a.active IS NULL OR a.active = TRUE")
    List<Assessment> findActiveOrUnspecified();

    Optional<Assessment> findFirstByTitleIgnoreCase(String title);
}
